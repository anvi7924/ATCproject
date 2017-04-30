#!/bin/bash

scp -i load-balancer.pem out/artifacts/server_jar/server.jar \
  ubuntu@ec2-54-69-74-202.us-west-2.compute.amazonaws.com:/home/ubuntu/user
scp -i load-balancer.pem out/artifacts/server_jar/server.jar \
  ubuntu@ec2-52-10-255-167.us-west-2.compute.amazonaws.com:/home/ubuntu/user
scp -i load-balancer.pem out/artifacts/server_jar/server.jar \
  ubuntu@ec2-35-165-212-137.us-west-2.compute.amazonaws.com:/home/ubuntu/user