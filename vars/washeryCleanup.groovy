package com.base2.ciinabox

/************************************
washery DSL


example usage
washeryCleanup(
    region: 'ap-southeast-2', // (required)
    accountId: '00000000000', // (required)
    role: 'role-name', // (required)
    prefix: 'washery-scrubbed', // (optional, snaphot name's prefix to filter)
    keepVersions: 5, // (conditional, required if keepDays is not set, keep last N snapshots)
    keepDays: 7 // (optional, required if keepVersions is not set, keep snapshots from last N days)
)
************************************/

import com.amazonaws.services.docdb.model.DescribeDBClusterSnapshotAttributesRequest
import com.base2.ciinabox.aws.AwsClientBuilder

def call(body) {
  def config = body

  def clientBuilder = new AwsClientBuilder([
    region: config.region,
    awsAccountId: config.get('accountId', null),
    role: config.get('role', null)
  ])

  def client    = clientBuilder.rds()
  def request   = new DescribeDBClusterSnapshotAttributesRequest().withSnapshotType('manual')
  def response  = client.describeDBClusterSnapshotAttributes(request)
  def snapshots = response.getDBClusterSnapshots()

  for (snapshot in snapshots) {
    println snapshot.toString()
    println snapshot.getDBClusterSnapshotArn()
  }
}

// #!/usr/bin/env python3

// import json
// import boto3
// from datetime import datetime, timedelta, timezone
// retentionDate = datetime.now(timezone.utc) - timedelta(days=7)
// print("Connecting to RDS")
// client = boto3.client('rds', region_name='us-west-2')
// rdssnapshots = client.describe_db_cluster_snapshots(
//     DBClusterSnapshotIdentifier='',
//     SnapshotType='manual',
// )

// print('Deleting all DB Washery Snapshots older than %s' % retentionDate)

// for snapshot in rdssnapshots['DBClusterSnapshots']:
//     if snapshot['DBClusterSnapshotIdentifier'].startswith('washery-scrubbed') and snapshot['SnapshotCreateTime'] < retentionDate:
//         print(snapshot["DBClusterSnapshotIdentifier"])
//         client.delete_db_cluster_snapshot(DBClusterSnapshotIdentifier=snapshot["DBClusterSnapshotIdentifier"])
