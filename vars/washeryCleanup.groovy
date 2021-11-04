/************************************
washery DSL


example usage
washeryCleanup(
    region: 'ap-southeast-2', // (required)
    accountId: '00000000000', // (optional, provide if assuming role in another account)
    role: 'role-name', // (optional, provide if assuming role in another account)
    sessionDuration: 3600 // (optional, extend the assume role session duration if need be. defaults to 3600)
    snapshotId: 'snapshot-name', // (required, rds snapshot name or arn)
    sqlScript: 'scrubber.sql', // (optional, anonymiser sql script to execute against the dataset)
    scriptBucket: 's3-bucket-name', // (conditional, required if sqlScript is set)
    instanceType: 'instance|cluster', // (required, cluster if using aurora, instance for mysql|postgres|sql-server rds)
    instanceSize: 'db.t3.small', // (optional, overide the default instance sizes set by washery)
    dumpBucket: 's3-bucket-name', // (optional, specify if dumping database to a s3 bucket)
    saveSnapshot: true|false, // (optional, defaults to true. Determines if a snapshot is taken of the scrubbed database)
    containerImage: 'ghcr.io/base2services/washery:v2', // (optional, the docker image to run in fargate, defaults to ghcr.io/base2services/washery:v2)
    databases: ['mydb', 'anotherdb'] // (optional list of databases to dump, defaults to all databases)
)
************************************/

def call(body) {
  def config    = body
  def client    = clientBuilder.rds()
  def request   = new DescribeDBClusterSnapshotAttributesRequest().withSnapshotType('manual')
  def response  = client.describeDBClusterSnapshotAttributes(request)
  def snapshots = response.getDBClusterSnapshots()


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
