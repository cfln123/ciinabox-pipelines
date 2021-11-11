package com.base2.ciinabox

/************************************
washery DSL

example usage
washeryCleanup(
    region: 'ap-southeast-2', // (required)
    accountId: '00000000000', // (required)
    role: 'role-name', // (required)
    identifier: '^washery-scrubbed.*$', // (optional, snapshot name's identifier to filter, supports regex)
    tags: { 'washery:snapshotId': 'ˆ.*daily$' }, // (optional, snapshot's tags to filter, value supports regex)
    keepVersions: 5, // (conditional, required if keepDays is not set, keep last N snapshots)
    keepDays: 7 // (conditional, required if keepVersions is not set, keep snapshots from last N days)
    dryRun: true // (optional)
)
************************************/

/*

TODO:
  Enable filter tags functions once the aws-sdk plugin is updated
*/

import java.lang.Exception
import java.util.Calendar

import com.amazonaws.services.rds.model.DBClusterSnapshot

import com.amazonaws.services.rds.model.DescribeDBClusterSnapshotsRequest
import com.amazonaws.services.rds.model.DeleteDBClusterSnapshotRequest 
import com.base2.ciinabox.aws.AwsClientBuilder

def getExpireDate(days) {
  Calendar cal = Calendar.getInstance();
  cal.add(Calendar.DATE, -days);
  return cal.getTime()
}

@NonCPS
def filterAndSortSnapshots(snapshots, identifier, tags) {
  snapshots = snapshots
    .findAll { it ->
      if (!it.getDBClusterSnapshotIdentifier().matches(identifier)) {
        return false
      }

      return true // Remove once plugin's update is done
      
      for (t1 in tags) {
        def found = false

        print it.toString()

        for (t2 in it.getTagList()) {
          if (t1.key == t2.getName() && tag.getValue().matches(t2.value)) {
            found = true
            break
          }
        }

        if (!found) {
          return false
        }
      }

      return true
    }
    .sort { s1, s2 -> s1.getSnapshotCreateTime() <=> s2.getSnapshotCreateTime()  }
}

def getOlderSnapshots(snapshots, versions, dryRun) {
  def count       = snapshots.size() - versions
  def identifiers = []

  if (count <= 0) {
    println 'SKIPPED: Only ' + snapshots.size() + ' snapshots found, at least ' + (versions + 1) + ' is required.'
    return
  }

  for (def i = 0; i < count; i++) {
    def snapshot = snapshots.get(i)
    println snapshot.toString()
    println 'Clearing snapshot: ' + snapshot.getDBClusterSnapshotArn()

    identifiers << snapshot.getDBClusterSnapshotIdentifier()
  }

  return identifiers
}

def getExpiredSnapshots(snapshots, days, dryRun) {
  def expireDate  = getExpireDate(days)
  def identifiers = []

  for (def i = 0; i < snapshots.size(); i++) {
    def snapshot = snapshots.get(i)

    if (expireDate < snapshot.getSnapshotCreateTime()) {
      if (i == 0) {
        println 'SKIPPED: No snapshots older than ' + days + ' days found.'
      }

      return identifiers
    }

    identifiers << snapshot.getDBClusterSnapshotIdentifier()
  }

  return identifiers
}

def call(body) {
  def config      = body
  def identifier  = config.get('identifier', '^copy-washery-scrubbed.*$')
  def versions    = config.get('keepVersions', 0)
  def days        = config.get('keepDays', 30)
  def dryRun      = config.get('dryRun', false)
  def tags        = config.get('tags', {
    washery:snapshotId: 'ˆ.*daily$'
  })

  def clientBuilder = new AwsClientBuilder([
    region: config.region,
    awsAccountId: config.get('accountId', null),
    role: config.get('role', null)
  ])

  def client  = clientBuilder.rds()
  def request = new DescribeDBClusterSnapshotsRequest()

  request.setSnapshotType("manual")

  def snapshotsResult = client.describeDBClusterSnapshots(request)
  def snapshots       = filterAndSortSnapshots(snapshotsResult.getDBClusterSnapshots(), identifier, tags)
  def identifiers     = []

  if (versions > 0) {
    identifiers = getOlderSnapshots(snapshots, versions, dryRun)
  } else if (days > 0) {
    identifiers = getExpiredSnapshots(snapshots, days, dryRun)
  } else {
    throw new Exception('Either keepVersions or keepDays must be set, as a valid integer and greater than 0')
  }

  if (dryRun) {
    println 'SKIPPED: Dry run. Snaphots: ' + identifiers.toString() 
  } else {
    for (id in identifiers) {
      println 'Clearing snapshot: ' + id

      def deleteRequest = new DeleteDBClusterSnapshotRequest().withDBClusterSnapshotIdentifier(id)
      client.deleteDBClusterSnapshot(deleteRequest)
    }
  } 
}