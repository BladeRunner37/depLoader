# depLoader
Download artifacts from Maven Central, pack to zip

1. Add dependencies to build.gradle with scope 'compile'. There is 'logstash-logback-encoder' added for example.
2. Run ./gradlew clean build.
3. After successfull build run Main.main()
4. Profit! There is 'toNexus.zip' in project root with all artifacts you need to load to your own nexus repository.

NOTE.
Dependencies

  group: 'org.apache.httpcomponents', name: 'httpclient', version: '4.5.5'
  
  group: 'org.zeroturnaround', name: 'zt-zip', version: '1.13'
  
  group: 'org.slf4j', name: 'slf4j-simple', version: '1.7.25'

will be ignored by walkGraph task, they are needed for this project only. If you want to resolve artifacts for this 
dependencies too, you have to remove it from exclusions - build.gradle:52
