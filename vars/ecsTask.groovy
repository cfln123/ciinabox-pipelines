
/************************************
ecsService (
  action: 'runAndWait',
  taskDefinition: 'example-task-definition',
  cluster: 'example-cluster',
  region: 'us-east-1',
  accountId: '12345678',
  role: 'ciinabox',
)
************************************/

@Grab(group='com.amazonaws', module='aws-java-sdk-ecs', version='1.11.359')
@Grab(group='com.amazonaws', module='aws-java-sdk-iam', version='1.11.359')
@Grab(group='com.amazonaws', module='aws-java-sdk-sts', version='1.11.359')

import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.services.ecs.*
import com.amazonaws.services.ecs.model.DescribeTasksRequest
import com.amazonaws.services.ecs.model.RunTaskRequest

import java.util.concurrent.*

def call(body) {
  def config = body
  def client = setupECSClient(config.region, config.accountId, config.role)

  config.wait = config.get('wait', false)

  handleActionRequest(client, config)
}

def handleActionRequest(client, config) {
  def success = true

  switch (config.action) {
    case 'runAndWait':
      def startedTasks = startTask(client, config)
      success = waitForTask(client, config, startedTasks)
      break
    case default:
      throw new GroovyRuntimeException("The specified action '${config.action}' is not implemented.")
  }
}

@NonCPS
def startTask(client, config) {
  def taskRequest = new RunTaskRequest()
  taskRequest.withCluster(config.cluster)
  taskRequest.launchType = config.launchType ?: "EC2"
  taskRequest.taskDefinition = config.taskDefinition

  return client.runTask(taskRequest)
}

@NonCPS
def waitForTask(client, config, startedTasks) {
  def describeTasksRequest = new DescribeTasksRequest()
  describeTasksRequest.withCluster(config.cluster)
  describeTasksRequest.withTasks(startedTasks.tasks.first().taskArn)

  while(true) {
    sleep 1000
    def taskDescriptions = client.describeTasks(describeTasksRequest)
    if (taskDescriptions.tasks.size() != 1) {
      return false
    }
    if (taskDescriptions.tasks.first().lastStatus == 'STOPPED') {
      return true
    }
  }
}

@NonCPS
def setupECSClient(region, awsAccountId = null, role = null) {
  def cb = AmazonECSClientBuilder.standard().withRegion(region)
  def creds = getCredentials(awsAccountId, region, role)
  if(creds != null) {
    cb.withCredentials(new AWSStaticCredentialsProvider(creds))
  }
  return cb.build()
}

@NonCPS
def getCredentials(awsAccountId, region, roleName) {
  def env = System.getenv()
  if(env['AWS_SESSION_TOKEN'] != null) {
    return new BasicSessionCredentials(
      env['AWS_ACCESS_KEY_ID'],
      env['AWS_SECRET_ACCESS_KEY'],
      env['AWS_SESSION_TOKEN']
    )
  } else if(awsAccountId != null && roleName != null) {
    def stsCreds = assumeRole(awsAccountId, region, roleName)
    return new BasicSessionCredentials(
      stsCreds.getAccessKeyId(),
      stsCreds.getSecretAccessKey(),
      stsCreds.getSessionToken()
    )
  } else {
    return null
  }
}

@NonCPS
def assumeRole(awsAccountId, region, roleName) {
  def roleArn = "arn:aws:iam::" + awsAccountId + ":role/" + roleName
  def roleSessionName = "sts-session-" + awsAccountId
  println "assuming IAM role ${roleArn}"
  def sts = new AWSSecurityTokenServiceClient()
  if (!region.equals("us-east-1")) {
      sts.setEndpoint("sts." + region + ".amazonaws.com")
  }
  def assumeRoleResult = sts.assumeRole(new AssumeRoleRequest()
            .withRoleArn(roleArn).withDurationSeconds(3600)
            .withRoleSessionName(roleSessionName))
  return assumeRoleResult.getCredentials()
}