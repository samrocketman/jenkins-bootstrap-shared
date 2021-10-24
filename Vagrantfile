# -*- mode: ruby -*-
# vi: set ft=ruby :

# Execute shell script before running vagrant.  This script will run on every
# vagrant command.
system('./scripts/vagrant-up.sh')

Vagrant.configure("2") do |config|
  # https://docs.vagrantup.com.
  config.vm.box = "centos/7"
  config.vm.network "forwarded_port", guest: 8080, host: 8080, host_ip: "127.0.0.1"
  config.vm.provider "virtualbox" do |vb|
    vb.memory = "4096"
  end
  if Vagrant.has_plugin?("vagrant-vbguest")
    config.vbguest.auto_update = false
  end
  config.vm.provision "shell", inline: <<-SHELL
    set -ex

    # temporarily disable SELinux
    sudo sed -i 's/^SELINUX=.*/SELINUX=disabled/' /etc/selinux/config
    sudo setenforce 0

    # install Java
    yum install -y java-1.8.0-openjdk-devel.x86_64 git
    #install Jenkins
    rpm -i /vagrant/build/distributions/*.rpm
    echo 'JAVA_HOME=/etc/alternatives/java_sdk' >> /etc/sysconfig/jenkins
    #start the Jenkins daemon
    systemctl start jenkins.service
  SHELL
end
