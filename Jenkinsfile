pipeline {
	agent { label "java17" }
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
						sh 'mvn package docker:build docker:push -DskipTests=true -Dbuild.number=$BUILD_NUMBER'
					}
				}
			}
		}
		stage('Update Helm Chart') {
			when { expression { params.push_docker_image == true } }
			steps {
				build job: 'z_IaC/update_netex_version', parameters: [string(name: 'minor_version', value: env.BUILD_NUMBER)], wait: true
			}
		}
	}
}
