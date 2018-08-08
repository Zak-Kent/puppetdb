repo_config_dir = 'tmp/repo_configs'
# TODO figure out a better way to share this information across steps
build_version = test_config[:install_mode] == :upgrade_oldest ? "4.4.3" : test_config[:package_build_version]
if (test_config[:install_type] == :package \
   and test_config[:package_build_version] \
   and not test_config[:skip_presuite_provisioning])
then
  # do not install the dev_repo if a package_build_version has not been specified.
  databases.each do |database|
    install_puppetlabs_dev_repo database, 'puppetdb', build_version,
                                repo_config_dir, options
  end
end