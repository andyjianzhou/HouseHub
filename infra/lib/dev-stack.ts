import * as cdk from 'aws-cdk-lib';
import { Construct } from 'constructs';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import * as iam from 'aws-cdk-lib/aws-iam';
import * as rds from 'aws-cdk-lib/aws-rds';
import * as s3 from 'aws-cdk-lib/aws-s3';

export class HouseHubDevStack extends cdk.Stack {
  constructor(scope: Construct, id: string, props?: cdk.StackProps) {
    super(scope, id, props);

    // 1. Create a VPC with only public subnets (no NAT Gateway)
    const vpc = new ec2.Vpc(this, 'DevVPC', {
      maxAzs: 2,
      natGateways: 0,
      subnetConfiguration: [
        {
          name: 'public',
          subnetType: ec2.SubnetType.PUBLIC,
        },
        {
          name: 'private',
          subnetType: ec2.SubnetType.PRIVATE_ISOLATED,  // Changed to PRIVATE_ISOLATED since there's no NAT
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

    // Creating RDS security group
    const dbSecurityGroup = new ec2.SecurityGroup(this, 'DbSecurityGroup', {
      vpc,
      description: 'Allow database access from app',
      allowAllOutbound: false,
    });
    dbSecurityGroup.addIngressRule(appSecurityGroup, ec2.Port.tcp(5432), 'Allow Postgres only from App on port 5432');

    // 3. Create S3 bucket
    const bucket = new s3.Bucket(this, 'MyBucket', {
      versioned: false,
      removalPolicy: cdk.RemovalPolicy.DESTROY,
      autoDeleteObjects: true,
      blockPublicAccess: s3.BlockPublicAccess.BLOCK_ALL,
      cors: [
        {
          allowedMethods: [s3.HttpMethods.GET, s3.HttpMethods.PUT, s3.HttpMethods.POST],
          allowedOrigins: ['*'],
          allowedHeaders: ['*'],
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
        subnetType: ec2.SubnetType.PRIVATE_ISOLATED,  // Changed to PRIVATE_ISOLATED
      },
      securityGroups: [dbSecurityGroup],
      databaseName: 'househub',
      credentials: rds.Credentials.fromGeneratedSecret('postgres'), // Automatically generate a username and password
      allocatedStorage: 20, 
      deleteAutomatedBackups: true,
      removalPolicy: cdk.RemovalPolicy.DESTROY
    });

    // 5. IAM Role for EC2 instance
    const ec2Role = new iam.Role(this, 'Ec2InstanceRole', {
      assumedBy: new iam.ServicePrincipal('ec2.amazonaws.com'),
    });

    bucket.grantReadWrite(ec2Role);

    // Grant Secrets Manager access - ADD THIS SECTION
    ec2Role.addManagedPolicy(iam.ManagedPolicy.fromAwsManagedPolicyName('AmazonRDSReadOnlyAccess'));
    const secretsManagerPolicy = new iam.PolicyStatement({
      effect: iam.Effect.ALLOW,
      actions: [
        'secretsmanager:GetSecretValue',
        'secretsmanager:DescribeSecret',
        'secretsmanager:ListSecrets'
      ],
      resources: ['*']  // For production, scope this down to specific secrets
    });
    ec2Role.addToPolicy(secretsManagerPolicy);

    // Also grant access to CloudFormation outputs
    const cloudFormationPolicy = new iam.PolicyStatement({
      effect: iam.Effect.ALLOW,
      actions: [
        'cloudformation:DescribeStacks'
      ],
      resources: ['*']
    });
    ec2Role.addToPolicy(cloudFormationPolicy);

    // 6. Create EC2 Instance (in public subnet)
    const instance = new ec2.Instance(this, 'DevInstance', {
      vpc,
      vpcSubnets: { subnetType: ec2.SubnetType.PUBLIC },
      instanceType: ec2.InstanceType.of(ec2.InstanceClass.T2, ec2.InstanceSize.MICRO),
      machineImage: ec2.MachineImage.latestAmazonLinux2023(),
      securityGroup: appSecurityGroup,
      keyName: 'HouseHubKeyStaging',
      role: ec2Role
    });

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

