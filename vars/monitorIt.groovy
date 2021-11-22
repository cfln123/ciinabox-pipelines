import com.base2.ciinabox.aws.IntrinsicsYamlConstructor
import org.yaml.snakeyaml.Yaml

def call(body) {
  def config    = body
  def types     = config.get('serviceTypes', false)
  def duration  = config.get('sessionDuration', 900)
  def resources = []
  def template  = ''
  
  if(!fileExists('./monitorable')) {
    sh(script: 'rm -rf monitorable && git clone https://github.com/cfln123/monitorable.git', label: 'monitorIt')
    sh(script: 'python3 -m pip install --user -r ./monitorable/requirements.txt', label: 'monitorIt')
  }

  withAWS(region: config.region, role: config.role, roleAccount: config.accountId, duration: duration, roleSessionName: 'monitorIt') {  
    dir('./monitorable') { 
      def monitorable = sh(script: "./monitorable.py --format cfn-guardian --regions $config.region", label: 'monitorIt', returnStdout: true)
      
      Yaml yaml = new Yaml(new IntrinsicsYamlConstructor())
      template = yaml.load(monitorable.split('### cfn-guardian config ###')[1]).Resources
    }
  }

  println '*** Services: ***'
  
  template.each { group, _resources ->
    _resources.each { resource -> 
      resources << [ Id: resource.Id, group: group ]
    }
  }

  println resources
}