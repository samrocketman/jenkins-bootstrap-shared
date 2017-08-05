# -*- mode: ruby -*-
# vi: set ft=ruby :

# Execute shell script before running vagrant.  This script will run on every
# vagrant command.
system('./scripts/vagrant-up.sh')

# All Vagrant configuration is done below. The "2" in Vagrant.configure
# configures the configuration version (we support older styles for
# backwards compatibility). Please don't change it unless you know what
# you're doing.
Vagrant.configure("2") do |config|
  # The most common configuration options are documented and commented below.
  # For a complete reference, please see the online documentation at
  # https://docs.vagrantup.com.

  # Every Vagrant development environment requires a box. You can search for
  # boxes at https://vagrantcloud.com/search.
  config.vm.box = "centos/7"

  # Create a forwarded port mapping which allows access to a specific port
  # within the machine from a port on the host machine and only allow access
  # via 127.0.0.1 to disable public access
  config.vm.network "forwarded_port", guest: 8080, host: 8080, host_ip: "127.0.0.1"

  # Share an additional folder to the guest VM. The first argument is
  # the path on the host to the actual folder. The second argument is
  # the path on the guest to mount the folder. And the optional third
  # argument is a set of non-required options.
  # config.vm.synced_folder "../data", "/vagrant_data"

  config.vm.provider "virtualbox" do |vb|
    vb.memory = "4096"
  end

  config.vm.provision "shell", inline: <<-SHELL
    #Download Java from Oracle
    [ ! -f /tmp/jdk8.rpm ] && curl -H 'Cookie: oraclelicense=accept-securebackup-cookie' -Lo /tmp/jdk8.rpm http://download.oracle.com/otn-pub/java/jdk/8u144-b01/090f390dda5b47b9b721c7dfaa008135/jdk-8u144-linux-x64.rpm
    #validate the download from Oracle using official checksum
    echo 'cdb016da0c509d7414ee3f0c15b2dae5092d9a77edf7915be4386d5127e8092f  /tmp/jdk8.rpm' | sha256sum -c -
    #Install Oracle JDK 8
    rpm -i /tmp/jdk8.rpm
    #install Jenkins
    rpm -i /vagrant/build/distributions/*.rpm
    #start the Jenkins daemon
    /etc/init.d/jenkins start
  SHELL
end
