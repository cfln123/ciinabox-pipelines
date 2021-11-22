def call(body) {
  def config    = body
  def types     = config.get('serviceTypes', false)
  def duration  = config.get('sessionDuration', 900)
  def resources = ''
  
  withAWS(region: config.region, role: config.role, roleAccount: config.accountId, duration: duration, roleSessionName: 'monitorIt') {
    
    // sh(script: 'rm -rf monitorable && git clone https://github.com/base2Services/monitorable.git', label: 'monitorIt')
    dir('/monitorable') {
    //   sh(script: "python3 -m pip install -r requirements.txt", label: 'monitorIt')
    resources = sh(script: "./monitorable.py --format cfn-guardian --regions $config.region", label: 'monitorIt')
    }
  }

  println '*** Services: ***'
  println resources
}