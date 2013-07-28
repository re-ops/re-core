import ch.qos.logback.core.FileAppender
import static ch.qos.logback.classic.Level.*
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.core.ConsoleAppender

appender('console', ConsoleAppender) {
    encoder(PatternLayoutEncoder) {
	pattern = "%level %logger [%thread] - %msg%n"
    }
}

appender('file', FileAppender) {
  if(new File('/var/log/').canWrite()){
    file = '/var/log/celestial-third-party.log'
  } else {
    file = 'celestial-third-party.log'
  }
  append = true
  encoder(PatternLayoutEncoder) {
	pattern = "%level %logger [%thread] - %msg%n"
  }
}

logger('net.schmizz.sshj.transport.verification.OpenSSHKnownHosts',ERROR, ['file'])

root(INFO,['file'])
