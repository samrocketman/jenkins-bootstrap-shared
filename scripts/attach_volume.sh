#!/bin/bash
#Created by Sam Gleske
#Thu Feb 13 21:10:20 PST 2016
#Linux 4.13.0-32-generic x86_64
#GNU bash, version 4.3.48(1)-release (x86_64-pc-linux-gnu)

# DESCRIPTION:
#     Search for AWS volumes based on a tag and attach it to the AWS instance where
#     this script is run.  This assumes host IAM role permission to do so on the
#     instance.
#
#     1. Find volume based on tags provided and attach it.
#     2. If the volume is raw disk, then it will be formatted to XFS.
#     3. Mount the volume to --mount-path.
#     4. Take ownership of the --mount-path only if it was formatted.
#     5. Add an entry to /etc/fstab to cover auto-mount on reboot.
#
#     The following example is based on the docker container generated from
#     this repository.  The UID and GID of the jenkins user inside the docker
#     container.
#
# EXAMPLE:
#     attach_volume.sh --volume-tags project=jenkins environment=prod half=blue --owner 100:65533 --mount-path /var/lib/jenkins

function fail() {
    echo "ERROR: $*" >&1
    exit 1
}

set -auxeEo pipefail
export PATH="/usr/local/bin:$PATH"
type -P jq || fail 'jq cli utility is missing.'
type -P aws || fail 'aws cli utility is missing.'
export AWS_DEFAULT_REGION="${AWS_DEFAULT_REGION:-us-east-1}"
VOLUME_DEVICE="/dev/xvdf"

set +x

function usage() {
    cat <<EOF
${0##*/} --volume-tags KEY=VALUE [KEY=VALUE...] --mount-point PATH [--device PATH] [--owner CHOWN_VALUE] [--region AWS_REGION]

DESCRIPTION:
    This script will automatically:

      - Find an EBS volume ID given a list of tags to match.
      - Attach the first volume found to this instance.
      - Format the volume to XFS only if it is raw disk.
      - Mount the formatted volume to a path.
      - Change permissions of the volume only if it was formatted.
      - Add an entry to /etc/fstab for auto-mounting the volume on instance
        restart.

    This script is meant to be run from an instance within AWS.  The host needs
    to have the ability to search for volumes, attach volumes, and detact
    volumes.  Instances can be security scoped to only allow volumes matching a
    certain tag.

REQUIRED OPTIONS:
    --volume-tags KEY=VALUE  One or more key value pairs which will be used to
                             search volume tags to find a volume ID.

    --mount-path PATH        A destination mounting point where the volume
                             device will be mounted after it is formatted.

OPTIONAL OPTIONS:
    --device PATH            A local raw device which is the destination of the
                             attached volume.  Default is /dev/vda.

    --owner CHOWN_VALUE      An owner of the formated path once the volume is
                             mounted.  This supports any value that chown
                             command supports for changing ownership.
    --region AWS_REGION      An AWS region in which the aws cli will operate
                             commands on for searching the volume.
                             Alternatively, the AWS_DEFAULT_REGION environment
                             variable can be set.  The default region set is
                             us-east-1.
EOF
}

tags=()
MOUNT_PATH=""
CHOWN_VALUE=""
while [ $# -gt 0 ]; do
    case "$1" in
        --help|-h)
            usage
            exit
            ;;
        --volume-tags)
            [ -n "${2:-}" ] || fail "$1 has blank or no arguments following it: $1 key=value ..."
            shift
            while grep -v -- "^--" <<< "${1:-}" | grep -F -- '=' > /dev/null; do
                tags+=( "$1" )
                shift
            done
            ;;
        --device)
            VOLUME_DEVICE="${2:-}"
            shift
            shift
            ;;
        --region)
            AWS_DEFAULT_REGION="${2:-}"
            shift
            shift
            ;;
        --mount-path)
            MOUNT_PATH="${2:-}"
            shift
            shift
            ;;
        --owner)
            CHOWN_VALUE="${2:-}"
            shift
            shift
            ;;
        *)
            fail "ERROR: encountered invalid argument $1"
            ;;
    esac
done

[ -n "${tags[*]:-}" ] || fail "--volume-tags has no arguments following it: --volume-tags key=value ..."
[ -n "${VOLUME_DEVICE}" ] || fail "--device PATH must be specified"
[ -n "${AWS_DEFAULT_REGION}" ] || fail "--region AWS_REGION must be specified or set environment variable AWS_DEFAULT_REGION"
[ -n "${MOUNT_PATH}" ] || fail "--mount-path PATH is a required option."

function get_volume_id() (
    local filters=()
    for tag in "$@"; do
        filters+=( "Name=tag:${tag%=*},Values=${tag#*=}" )
    done
    aws ec2 describe-volumes --filters "${filters[@]}" | jq -r '.Volumes[0].VolumeId'
)

function wait_for_volume_detachment() {
    until [ "$(aws ec2 describe-volumes --volume-ids "${1:-}" | jq -r '.Volumes[0].State')" = available ]; do
        echo "Waiting for volume ${1:-} to be detached..."
        sleep 1
    done
}

function wait_for_volume_attachment() {
    until [ "$(aws ec2 describe-volumes --volume-ids "${1:-}" | jq -r '.Volumes[0].Attachments[0].State')" = attached ]; do
        echo "Waiting for volume ${1:-} to be attached..."
        sleep 1
    done

    until [ -e "${VOLUME_DEVICE}" ]; do
        echo "Waiting for disk ${VOLUME_DEVICE} to become available."
        sleep 1
    done
}

if mount | grep -F -- "${MOUNT_PATH}"; then
    echo "${MOUNT_PATH} is already mounted with a device." >&2
    exit
fi

INSTANCE_ID="$(curl -s http://169.254.169.254/latest/meta-data/instance-id)"
VOLUME_ID="$(get_volume_id "${tags[@]}")"

if [[ -z "${VOLUME_ID}" || "${VOLUME_ID}" = null ]]; then
    fail "A VOLUME_ID could not be determined with the given --volume-tags ${tags[*]:-}"
fi

if [ -e "${VOLUME_DEVICE}" ]; then
    echo "Volume device already exists at ${VOLUME_DEVICE}, skipping attach volume ${VOLUME_ID}"
else
    wait_for_volume_detachment "${VOLUME_ID}"
    aws ec2 attach-volume --instance-id "${INSTANCE_ID}" --device "${VOLUME_DEVICE}" --volume-id "${VOLUME_ID}"
    wait_for_volume_attachment "${VOLUME_ID}"
fi

if [ ! -e "${VOLUME_DEVICE}" ]; then
    echo "ERROR: although you requested $VOLUME_ID to be attached as $VOLUME_DEVICE the Linux Kernel renamed your volume device to some other name." >&2
    echo "See article: https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/device_naming.html" >&2
    echo "Known attached devices:" >&2
    cat /proc/partitions
    fail "Unable to proceed because we don't know the device of the attached volume."
fi

REAL_DEVICE="$(readlink -f "${VOLUME_DEVICE}")"
REQUIRED_FORMAT=no
if ! blkid | grep -F -- "${REAL_DEVICE}:"; then
    REQUIRED_FORMAT=yes

    # agcount is for allocation groups for XFS parallel performance
    # in general it should be the count of the processors on the system.
    # For Jenkins scale, we expect around 4-16 CPU cores so I'm setting it to 8
    # as a happy medium.
    mkfs.xfs -K -d agcount=8 "${REAL_DEVICE}"
fi

mount "${VOLUME_DEVICE}" "${MOUNT_PATH}"

if [[ "${REQUIRED_FORMAT}" = yes && -n "${CHOWN_VALUE}" ]]; then
    chown -- "${CHOWN_VALUE}" "${MOUNT_PATH}"
fi

grep -F -- "${MOUNT_PATH}" /etc/fstab ||
    echo "${VOLUME_DEVICE} ${MOUNT_PATH} xfs noatime,nodiratime,nobarrier 0 0" |
    tee -a /etc/fstab
