cd dist
tar -zcvf launch.tar.gz LaunchServer.jar lib/
scp -i ../../Key1.pem launch.tar.gz ec2-user@ec2-18-130-21-31.eu-west-2.compute.amazonaws.com:~/launch
cd ..
