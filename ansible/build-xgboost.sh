#!/usr/bin/env bash

sudo mkdir /opt/java && cd /opt/java
wget --no-cookies --no-check-certificate --header "Cookie: gpw_e24=http%3A%2F%2Fwww.oracle.com%2F; oraclelicense=accept-securebackup-cookie" "http://download.oracle.com/otn-pub/java/jdk/8u45-b14/jdk-8u45-linux-x64.tar.gz"
tar -zxvf /opt/java/jdk-8u45-linux-x64.tar.gz

export JAVA_HOME=/opt/java/jdk1.8.0_45
export PATH=$PATH:$JAVA_HOME

sudo yum -y install git-all

cd /home/ec2-user

git clone --recursive https://github.com/dmlc/xgboost

sudo wget http://mirror.nbtelecom.com.br/apache/maven/maven-3/3.3.9/binaries/apache-maven-3.3.9-bin.zip -O /opt/apache-maven-3.3.9-bin.zip

cd /opt
sudo unzip apache-maven-3.3.9-bin.zip
export PATH=$PATH:/opt/apache-maven-3.3.9/bin

cd /home/ec2-user/xgboost/jvm-packages/
mvn package install -DskipTests

cp /home/ec2-user/xgboost/jvm-packages/xgboost4j/target/xgboost4j-0.7-jar-with-dependencies.jar /tmp/xgboost4j-0.7-jar-with-dependencies.jar
cp /home/ec2-user/xgboost/jvm-packages/xgboost4j-spark/target/xgboost4j-spark-0.7-jar-with-dependencies.jar /tmp/xgboost4j-spark-0.7-jar-with-dependencies.jar