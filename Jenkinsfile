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
					sh 'mvn -f netex-importer clean install'
				}
			}
		}
		stage('Deploy') {
			when { expression { params.push_docker_image == true } }
			steps {
				withCredentials([usernamePassword(credentialsId: 'bernmobil_harbor_robots_credentials', passwordVariable: 'REGISTRY_PASSWORD', usernameVariable: 'REGISTRY_USERNAME')]) {
					withMaven(maven: '3.9.6') {
						sh 'mvn -f netex-importer docker:build docker:push'
					}
				}
			}
		}
	}
}
