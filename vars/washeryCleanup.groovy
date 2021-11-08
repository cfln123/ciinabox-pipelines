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

/*

TODO:
  Add dry run feature
  Fix getExpireDate function
  Do actual snapshot deletion

*/

import java.util.Date
import java.lang.Exception
import java.util.function

import com.amazonaws.services.rds.model.DBClusterSnapshot

//import com.amazonaws.services.docdb.model.DescribeDBClusterSnapshotsRequest
import com.base2.ciinabox.aws.AwsClientBuilder

def getExpireDate(days) {
  return new Date().now().getTime() + (86400 * days) // Arrumar
}

def filterAndSortSnapshots(snapshots) {
  def results = FluentIterable.from(snapshots).filter(new Predicate<DBClusterSnapshot>() {
    @Override
    public boolean apply(DBClusterSnapshot input) {
      return snapshot.getSnapshotType() == 'manual' && snapshot.getDBClusterSnapshotIdentifier().startsWith(prefix)
    }
    }).toSortedList(new Comparator<DBClusterSnapshot>() {
      @Override
      public int compare(DBClusterSnapshot s1, DBClusterSnapshot s2) {
        return s1.getSnapshotCreateTime().compareTo(s2.getSnapshotCreateTime());
      }
    });
}

def clearOlderSnapshots(snapshots, versions) {
  def count = 0

  for (def i = 0; i < versions; i++) {
    def snapshot = snapshots.get(i)
    println snapshot.toString()
    println 'Clearing snapshot: ' + snapshot.getDBClusterSnapshotArn()

    count++
  }

  return count
}

def clearExpiredSnapshots(snapshots, expireDate) {
  def count = 0

  for (def i = 0; i < snapshots.size(); i++) {
    def snapshot = snapshots.get(i)

    if (snapshot.getSnapshotCreateTime().getTime() > expireDate()) {
      return count
    }

    println snapshot.toString()
    println 'Clearing snapshot: ' + snapshot.getDBClusterSnapshotArn()
    
    count++
  }

  return count
}

def call(body) {
  def config     = body
  def prefix     = config.get('prefix', 'washery-scrubbed')
  def versions   = config.get('keepVersions', 0)
  def days       = config.get('keepDays', 0)

  def clientBuilder = new AwsClientBuilder([
    region: config.region,
    awsAccountId: config.get('accountId', null),
    role: config.get('role', null)
  ])

  def client  = clientBuilder.rds()

  /* 
    For unknown reasons (probably the version of the java-sdk since it couldnt find the codeartifact class is not implemented)
    we cant manage to make the DescribeDBClusterSnapshotsRequest work to filter the snapshots by type, doing it manully for now
  */

  //def request = new DescribeDBClusterSnapshotsRequest()
    //.withDBClusterIdentifier("carbon-dev-db-alaowgx15ood-dbcluster-1gwzozkft4ovq")
    //.withDBClusterIdentifier("")

  //request.setSnapshotType("manual")

  // def snapshotsResult = client.describeDBClusterSnapshots(request)
  def snapshotsResult = client.describeDBClusterSnapshots()
  def snapshots       = snapshotsResult.getDBClusterSnapshots()
  def deletedCount    = 0
  
  if (versions > 0) {
    deletedCount = versions - snapshots.size()
    clearOlderSnapshots(snapshots, deletedCount)
  } else if (days > 0) {
    def expireDate = getExpireDate(days)
    deletedCount   = clearExpiredSnapshots(snaphots, expireDate)
  } else {
    throw new Exception('Either keepVersions or keepDays must be set, as a valid integer and greater than 0')
  }

  if (deletedCount == 0) {
    println 'SKIPPED: Not enough version'
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
