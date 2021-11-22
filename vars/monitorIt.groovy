def call(body) {
  def config    = body
  def types     = config.get('serviceTypes', false)
  def duration  = config.get('sessionDuration', 900)
  def resources = ''
  
  withAWS(region: config.region, role: config.role, roleAccount: config.accountId, duration: duration, roleSessionName: 'monitorIt') {
    sh(script: 'git clone https://github.com/base2Services/monitorable.git', label: 'monitorIt')
    dir('./monitorable') {
      resources = sh(script: "./monitorable.py --format cfn-guardian --regions $config.region | sed '1,/^### cfn-guardian config ###$/d'", label: 'monitorIt')
    }
  }

  println '*** Services: ***'
  println resources
}