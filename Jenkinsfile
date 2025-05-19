pipeline {
    agent any

    environment {
        DOCKER_HUB_CREDENTIALS = credentials('dockerhub_credentials')
        DOCKER_IMAGE = "scotteinsteinwu/myapp"
        DOCKER_TAG = "${env.BUILD_NUMBER}"
    }

    stages {
        stage('Build') {
            steps {
                checkout scmGit(
                    branches: [[name: '*/master']],
                    extensions: [],
                    userRemoteConfigs: [[
                        url: 'https://github.com/Scott-Einstein/Teedy-2.git'
                    ]]
                )
                    sh 'mvn -B -DskipTests clean package'
            }
        }

        stage('Building image') {
            steps {
                script {
                    docker.build("${env.DOCKER_IMAGE}:${env.DOCKER_TAG}")
                }
            }
        }

        // Uploading Docker images into Docker Hub
        stage('Upload image') {
            steps {
                script {
                    docker.withRegistry('https://registry.hub.docker.com', 'DOCKER_HUB_CREDENTIALS') {
                        // Push the image to Docker Hub
                        docker.image("${env.DOCKER_IMAGE}:${env.DOCKER_TAG}").push()
                        // Optional: Label latest
                        docker.image("${env.DOCKER_IMAGE}:${env.DOCKER_TAG}").push('latest')
                    }
                }
            }
        }

        // Running Docker container
        stage('Run containers') {
            steps {
                script {
                    // stop then remove any existing container
                    sh 'docker stop teedy-container-8081 || true'
                    sh 'docker rm teedy-container-8081 || true'

                    // run the container
                    docker.image("${env.DOCKER_IMAGE}:${env.DOCKER_TAG}").run(
                        '--name teedy-container-8081 -d -p 8081:8080 '
                    )

                    // Optional: list all teedy-containers 
                    sh 'docker ps --filter "name=teedy-container"'
                }
            }
        }
    }
 }