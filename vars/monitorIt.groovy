import com.base2.ciinabox.aws.IntrinsicsYamlConstructor
import org.yaml.snakeyaml.Yaml

def call(body) {
  def config    = body
  def types     = config.get('serviceTypes', false)
  def duration  = config.get('sessionDuration', 900)
  def resources = ''
  
  withAWS(region: config.region, role: config.role, roleAccount: config.accountId, duration: duration, roleSessionName: 'monitorIt') {
    sh(script: 'rm -rf monitorable && git clone https://github.com/cfln123/monitorable.git', label: 'monitorIt')
    dir('./monitorable') {
      sh(script: "python3 -m pip install --user -r requirements.txt", label: 'monitorIt')
      
      def monitorable = sh(script: "./monitorable.py --format cfn-guardian --regions $config.region", label: 'monitorIt', returnStdout: true)
      
      Yaml yaml = new Yaml(new IntrinsicsYamlConstructor())
      resources = yaml.load(monitorable.split('### cfn-guardian config ###')[1])
    }
  }

  println '*** Services: ***'
  println resources
}