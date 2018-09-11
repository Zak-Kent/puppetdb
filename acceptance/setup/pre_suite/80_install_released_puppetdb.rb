# We skip this step entirely unless we are running in :upgrade mode.
# or if we're running against bionic on the 5.1.x branch
version = test_config[:package_build_version].to_s
latest_released = get_latest_released(version)

if ([:upgrade_oldest, :upgrade_latest].include? test_config[:install_mode] \
    and not test_config[:skip_presuite_provisioning] \
    and not (is_bionic and get_testing_branch(version) == '5.1.x'))

  puts("hahahahahahah")
  puts(test_config)
  if (test_config[:install_mode] == :upgrade_latest \
      and test_config[:nightly] == true)
    # if we're testing upgrade_latest and nightly_axis is true install real repo
    # without this the host doesn't have access to the repos it needs
    puts("inside the check for nightly builds")
    step "Install Puppet Labs repositories" do
      hosts.each do |host|
        initialize_repo_on_host(host, test_config[:os_families][host.name], false)
      end
    end
  end


  install_target = test_config[:install_mode] == :upgrade_latest ? latest_released : oldest_supported
  step "Install most recent released PuppetDB on the PuppetDB server for upgrade test" do
    databases.each do |database|
      enable_https_apt_sources(database)
      install_puppetdb(database, install_target)
      start_puppetdb(database)
      install_puppetdb_termini(master, databases, install_target)
    end
  end
end
