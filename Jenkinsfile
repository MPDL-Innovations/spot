node {
   stage 'Checkout'
   // Checkout code from repository
   checkout scm

   // Get the maven tool.
   // ** NOTE: This 'M3' maven tool must be configured
   // **       in the global configuration.
   def mvnHome = tool 'M3'


   stage 'Build'
   // Run the maven build
   sh "${mvnHome}/bin/mvn clean install"
   
   
   stage 'Deploy'
   echo "We are currently working on branch: ${env.BRANCH_NAME}"
   
    switch (env.BRANCH_NAME){
    	case 'dev':
    		echo "Deploy to dev";
    		break;
    	case 'qa':
    		echo "deploy to qa";
    		break;
    	default:
    		echo "no deployment";
    }
}