pipeline {
    agent any

    stages {
	
		
        
		
		        stage('deploy') {
            steps {
				       bat 'gradlew clean compile'
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
