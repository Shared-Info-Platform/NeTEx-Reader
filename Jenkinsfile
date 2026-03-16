pipeline {
	agent { label "java21-bookworm" }
	options {
		skipStagesAfterUnstable()
	}
	parameters {
		booleanParam(
			name: 'push_docker_image',
			description: 'Build and Push Docker Image?',
			defaultValue: false
		)
	}
	stages {
		stage('Build') {
			steps {
				withMaven(maven: '3.9.6') {
					sh 'mvn clean install -DmongoConnectionString=mongodb://mongodb-sip-hub:27027'
				}
			}
		}
		stage('Deploy') {
			when { expression { params.push_docker_image == true } }
			steps {
				withCredentials([usernamePassword(credentialsId: 'bernmobil_harbor_robots_credentials', passwordVariable: 'REGISTRY_PASSWORD', usernameVariable: 'REGISTRY_USERNAME')]) {
					withMaven(maven: '3.9.6') {
						sh 'mvn package docker:build docker:push -DskipTests=true -Dbuild.number=b$BUILD_NUMBER -Dgit.source=$(git rev-parse --short=7 HEAD)'
					}
				}
			}
		}
		stage('Update Helm Chart') {
			when { expression { params.push_docker_image == true } }
			steps{
                script {
                    withMaven(maven: '3.9.6') {
    			        def projectVersion = sh(
    				         script: "mvn help:evaluate -Dexpression=project.version -q -DforceStdout",
    				         returnStdout: true
							 ).trim()
    			        def appVersion = "${projectVersion}-b${env.BUILD_NUMBER}-${env.GIT_COMMIT.take(7)}" 

    			        build job: 'z_IaC/update_helmchart_appversion_final', 
    			        parameters: [
    				         string(name: 'chart_name', value: 'netex'),
    				         string(name: 'app_version', value: appVersion)
    			        ], 
    			        wait: true
                    }    
                }
            }
		}
	}
}
