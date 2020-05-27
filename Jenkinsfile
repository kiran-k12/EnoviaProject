pipeline {
    agent any

    stages {
	
		
        
		
		        stage('deploy') {
            steps {
				       bat 'gradlew clean deploy'
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