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
    keepDays: 7 // (conditional, required if keepVersions is not set, keep snapshots from last N days)
    dryRun: true // (optional)
)
************************************/

/*

TODO:
  Do actual snapshot deletion

*/

import java.util.Date
import java.lang.Exception
import java.util.stream.Collectors

import com.amazonaws.services.rds.model.DBClusterSnapshot

import com.amazonaws.services.docdb.model.DescribeDBClusterSnapshotsRequest
import com.base2.ciinabox.aws.AwsClientBuilder

def getExpireDate(days) {
  return new Date().now().getTime() + (86400 * days)
}
def _sort(s1, s2) {
  return 
}

def _filter(s) {
  return 
}
def filterAndSortSnapshots(snapshots, prefix) {
  return snapshots.stream()
    .filter({ s -> s.getSnapshotType() == 'manual' && s.getDBClusterSnapshotIdentifier().startsWith(prefix) })
    .sorted({ s1, s2 -> s1.getSnapshotCreateTime().compareTo(s2.getSnapshotCreateTime()) })
    .collect(Collectors.toList())
}

def clearOlderSnapshots(snapshots, versions, dryRun) {
  def count = versions - snapshots.size()

  if (count <= 0) {
    println 'SKIPPED: Only ' + snapshots.size() + ' snapshots found, at least ' + (versions + 1) + ' is required.'
    return
  }

  for (def i = 0; i < count; i++) {
    def snapshot = snapshots.get(i)
    println snapshot.toString()
    println 'Clearing snapshot: ' + snapshot.getDBClusterSnapshotArn()

    if (!dryRun) {
      //delete
    }
  }
}

def clearExpiredSnapshots(snapshots, days, dryRun) {
  def expireDate = getExpireDate(days)

  for (def i = 0; i < snapshots.size(); i++) {
    def snapshot = snapshots.get(i)

    if (snapshot.getSnapshotCreateTime().getTime() > expireDate) {
      if (i == 0) {
        println 'SKIPPED: No snapshots older than ' + days + ' days found.'
      }

      return
    }

    println snapshot.toString()
    println 'Clearing snapshot: ' + snapshot.getDBClusterSnapshotArn()

    if (!dryRun) {
      //delete
    }
  }
}

def call(body) {
  def config     = body
  def prefix     = config.get('prefix', 'washery-scrubbed')
  def versions   = config.get('keepVersions', 0)
  def days       = config.get('keepDays', 0)
  def dryRun     = config.get('dryRun', true)

  def clientBuilder = new AwsClientBuilder([
    region: config.region,
    awsAccountId: config.get('accountId', null),
    role: config.get('role', null)
  ])

  def client  = clientBuilder.rds()

  /* 
  // Waiting for the aws-sdk for the aws-sdk to be updated to implement this bit

  def request = new DescribeDBClusterSnapshotsRequest()
    .withDBClusterIdentifier("carbon-dev-db-alaowgx15ood-dbcluster-1gwzozkft4ovq")
    .withDBClusterIdentifier("")

  request.setSnapshotType("manual")

  def snapshotsResult = client.describeDBClusterSnapshots(request)

  */

  def snapshotsResult = client.describeDBClusterSnapshots()
  def snapshots       = filterAndSortSnapshots(snapshotsResult.getDBClusterSnapshots(), prefix)

  println snapshots.toString()

  return
  
  if (versions > 0) {
    clearOlderSnapshots(snapshots, versions, dryRun)
  } else if (days > 0) {
    clearExpiredSnapshots(snapshots, days, dryRun)
  } else {
    throw new Exception('Either keepVersions or keepDays must be set, as a valid integer and greater than 0')
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
