pipeline {
    agent any

    stages {
	
		
        
		
		        stage('build') {
            steps {
				       bat 'gradlew clean build deploy'
					   bat 'echo "In cleanDeploy"'
				   }
				  }
				  

		
		
		
		stage('installSchema') {
            steps {
			   
			
				//InstallSchema
				bat './gradlew installSchema'
				bat 'echo "In installSchema"'
				
            }
        }
		

    }
}
