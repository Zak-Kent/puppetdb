require 'beaker/dsl/install_utils'

extend Beaker::DSL::InstallUtils

unless (test_config[:skip_presuite_provisioning])
  step "Install the puppetdb module and dependencies" do
    on databases, "puppet module install puppetlabs/puppetdb"


    on databases, "echo HHHHHHHHHHHHHHHHHHHHHHHHHH"
    on databases, "sed -i 's/gpgcheck=1/gpgcheck=0/g' /etc/dnf/dnf.conf"
    on databases, "dnf config-manager --add-repo https://download.postgresql.org/pub/repos/yum/9.6/redhat/rhel-8-x86_64/"
    on databases, "dnf clean all"
    on databases, "dnf install -y https://download.postgresql.org/pub/repos/yum/9.6/redhat/rhel-8-x86_64/postgresql96-9.6.12-1PGDG.rhel8.x86_64.rpm"
    on databases, "dnf install -y https://download.postgresql.org/pub/repos/yum/9.6/redhat/rhel-8-x86_64/postgresql96-server-9.6.12-1PGDG.rhel8.x86_64.rpm"


  end
end
