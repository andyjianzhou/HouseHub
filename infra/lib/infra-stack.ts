import * as cdk from 'aws-cdk-lib';
import { Construct } from 'constructs';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import * as iam from 'aws-cdk-lib/aws-iam';
import * as rds from 'aws-cdk-lib/aws-rds';
import * as s3 from 'aws-cdk-lib/aws-s3';
// import * as sqs from 'aws-cdk-lib/aws-sqs';

export class HouseHubDevStack extends cdk.Stack {
  constructor(scope: Construct, id: string, props?: cdk.StackProps) {
    super(scope, id, props);

    // The code that defines your stack goes here

    // 1. Create a VPC
    const vpc = new ec2.Vpc(this, 'DevVPC', {
      maxAzs: 2, // Default is all AZs in the region
      natGateways: 0,
      subnetConfiguration: [
        {
          name: 'public',
          subnetType: ec2.SubnetType.PUBLIC,
        },
        {
          name: 'private',
          subnetType: ec2.SubnetType.PRIVATE_WITH_EGRESS,
        },
      ]    
    });

    // 2. Create Security groups for the app instance
    const appSecurityGroup = new ec2.SecurityGroup(this, 'AppSecurityGroup', {
      vpc,
      description: 'Allow HTTP and SSH access',
      allowAllOutbound: true,
    });
    appSecurityGroup.addIngressRule(ec2.Peer.anyIpv4(), ec2.Port.tcp(22), 'Allow SSH on port 22');
    appSecurityGroup.addIngressRule(ec2.Peer.anyIpv4(), ec2.Port.tcp(8080), 'Allow Spring Boot on port 8080');

    // Creating RDS security group for RDS instance  
    const dbSecurityGroup = new ec2.SecurityGroup(this, 'DbSecurityGroup', {
      vpc,
      description: 'Allow database access from app',
      allowAllOutbound: false,
    });
    dbSecurityGroup.addIngressRule(appSecurityGroup, ec2.Port.tcp(5432), 'Allow Postgres only from App on port 5432'); 

    // 3. Create S3 bucket
    const bucket = new s3.Bucket(this, 'MyBucket', {
      versioned: false, // Set to true if you want versioning, versioning means you can have multiple versions of the same object in the bucket
      removalPolicy: cdk.RemovalPolicy.DESTROY, // NOT recommended for production code
      autoDeleteObjects: true, // NOT recommended for production code. Removal policy is set to destroy, so this will delete the objects in the bucket when the stack is deleted
      blockPublicAccess: s3.BlockPublicAccess.BLOCK_ALL, // Block all public access so the bucket is not publicly accessible, only to the app instance
      cors: [
        {
          allowedMethods: [s3.HttpMethods.GET, s3.HttpMethods.PUT, s3.HttpMethods.POST],
          // restrict origins in production
          // allowedOrigins: ['https://example.com'], // Replace with your frontend URL
          allowedOrigins: ['*'], // Allow all origins
          allowedHeaders: ['*'], // Allow all headers
        },
      ]
    });

    // Create RDS
    const dbInstance = new rds.DatabaseInstance(this, 'DevDatabase', {
      engine: rds.DatabaseInstanceEngine.postgres({
        version: rds.PostgresEngineVersion.VER_14,
      }),
      instanceType: ec2.InstanceType.of(ec2.InstanceClass.BURSTABLE3, ec2.InstanceSize.MICRO),
      vpc,
      vpcSubnets: {
        subnetType: ec2.SubnetType.PRIVATE_WITH_EGRESS,
      }, // VPC subnet is private because we want to access the database from the app instance
      securityGroups: [dbSecurityGroup], // Add the security group to the database instance
      databaseName: 'househub',
      credentials: rds.Credentials.fromGeneratedSecret('postgres'), // Automatically generate a username and password
      allocatedStorage: 20, // Minimum storage size in GB
      deleteAutomatedBackups: true,
      removalPolicy: cdk.RemovalPolicy.DESTROY  // Use SNAPSHOT
    });

    // 5. IAm Role for EC2 instance
    const ec2Role = new iam.Role(this, 'Ec2InstanceRole', {
      assumedBy: new iam.ServicePrincipal('ec2.amazonaws.com'),
    });

    // Grant S3 access
    bucket.grantReadWrite(ec2Role); // Grant the role read/write access to the bucket

    // 6. Create EC2 Instance
    const instance = new ec2.Instance(this, 'DevInstance', {
      vpc,
      vpcSubnets: { subnetType: ec2.SubnetType.PUBLIC }, // Publlic subnet for the instance so we can access it from the internet
      instanceType: ec2.InstanceType.of(ec2.InstanceClass.T2, ec2.InstanceSize.MICRO),
      machineImage: ec2.MachineImage.latestAmazonLinux2023(),
      securityGroup: appSecurityGroup,
      keyName: 'HouseHubKey', // Create this key pair in console
      role: ec2Role
    });

    // Add user data script to set up the instance
    instance.addUserData(
      '#!/bin/bash',
      'yum update -y',
      'yum install -y java-17-amazon-corretto',
      'mkdir -p /opt/househub'
    );

    // 7. Create Outputs
    new cdk.CfnOutput(this, 'BucketName', { value: bucket.bucketName });
    new cdk.CfnOutput(this, 'DatabaseEndpoint', { value: dbInstance.dbInstanceEndpointAddress });
    new cdk.CfnOutput(this, 'DatabasePort', { value: dbInstance.dbInstanceEndpointPort });
    new cdk.CfnOutput(this, 'EC2PublicIP', { value: instance.instancePublicIp });
  } 
}
