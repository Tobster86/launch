#----------------------------------------------
# Launch! server deployment script.
# Bundles and deploys Launch while running.
# Project must be fully built before executing.
#----------------------------------------------

server = "192.168.1.2"#"ec2-18-130-21-31.eu-west-2.compute.amazonaws.com"
user = "tobster"#"ec2-user"
remote_dir = ":~/launch"
key = ""#"../../Key1.pem"
password = ""

require 'rye'

Dir.chdir "dist"

puts "Bundling the release..."
puts `tar -zcvf launch.tar.gz LaunchServer.jar lib/`

if File.file?("launch.tar.gz")
    puts "Bundled okay. Transferring to server..."
    #puts `scp -i #{key} launch.tar.gz #{user}@#{server}#{remote_dir}`

    puts "Transferred to server. Logging on..."
    rye = Rye::Box.new(server, {:keys => key, :user => user, :password => password})

    puts "Logged on. Changing to directory..."
    puts rye.cd "launch"
    puts rye.ls
    puts rye.bash "screen -r"
    puts rye.bash "h"
    puts rye.bash "\ca"
    puts rye.bash "\cd"


else
    puts "ERROR: launch.tar.gz was not created properly."
end

#Log into EC2:
#scp -i ../../Key1.pem launch.tar.gz ec2-user@ec2-18-130-21-31.eu-#west-2.compute.amazonaws.com:~/launch
#
#Stop launch:
#screen -r
#quit
#ctrl-A D
#
#Unpack:
#cd launch/
#tar -zxvf launch.tar.gz
#rm launch.tar.gz
#
#Relaunch:
#screen -r
#java -jar LaunchServer.jar
#ctrl-A D
#
#exit
#exit
#
