pipeline {
    agent any

    stages {
	
		stage('Build') {
	
				steps {
		                bat 'gradlew clean build'
		              }
	            }

		stage('SonarQube analysis') {
      steps {
        
           
        withSonarQubeEnv('sonar') {
          bat 'gradlew clean sonar:sonar'
        
      }
    }
           }
		       				
    }
}
