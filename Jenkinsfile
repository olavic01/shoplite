// Multibranch pipeline.
//   feature/*, PRs : build + test + check images build  (no deploy)
//   develop        : same + auto-deploy to the app server
//   main           : same + manual approval, then deploy
//
// Jenkins setup needed (see docs/AWS-DEPLOYMENT.md):
//   - Credential  "app-server-ssh"  (SSH Username with private key, user ubuntu)
//   - Credential  "shoplite-env"    (Secret file: the app server's .env contents)
//   - Plugin      "SSH Agent"
//   - Global env var APP_HOST       (private IP of the app server)
pipeline {
  agent any
  options { timestamps(); disableConcurrentBuilds(); buildDiscarder(logRotator(numToKeepStr: '15')) }

  stages {
    stage('Backend: build & test') {
      steps { sh 'mvn -B -f services/pom.xml verify' }
      post { always { junit allowEmptyResults: true, testResults: 'services/**/target/surefire-reports/*.xml' } }
    }

    stage('Images build') {
      steps {
        sh '''
          for m in api-gateway user-service product-service order-service inventory-service notification-service; do
            docker build -q --build-arg MODULE=$m -t shoplite/$m:build-${BUILD_NUMBER} services
          done
          docker build -q -t shoplite/frontend:build-${BUILD_NUMBER} frontend
        '''
      }
    }

    stage('Approve production deploy') {
      when { branch 'main' }
      steps { timeout(time: 30, unit: 'MINUTES') { input message: 'Deploy this build to production?', ok: 'Deploy' } }
    }

    stage('Deploy') {
      when { anyOf { branch 'develop'; branch 'main' } }
      steps {
        sshagent(credentials: ['app-server-ssh']) {
          withCredentials([file(credentialsId: 'shoplite-env', variable: 'ENV_FILE')]) {
            sh '''
              rsync -az --delete -e "ssh -o StrictHostKeyChecking=accept-new" \
                --exclude .git --exclude node_modules --exclude target \
                ./ ubuntu@${APP_HOST}:~/shoplite/
              scp -o StrictHostKeyChecking=accept-new "$ENV_FILE" ubuntu@${APP_HOST}:~/shoplite/deploy/.env
              ssh -o StrictHostKeyChecking=accept-new ubuntu@${APP_HOST} \
                "cd ~/shoplite/deploy && IMAGE_TAG=build-${BUILD_NUMBER} docker compose up -d --build --remove-orphans"
            '''
          }
        }
      }
    }

    stage('Smoke test') {
      when { anyOf { branch 'develop'; branch 'main' } }
      steps {
        sh '''
          for i in $(seq 1 30); do
            code=$(curl -s -o /dev/null -w "%{http_code}" http://${APP_HOST}/api/products || true)
            [ "$code" = "200" ] && echo "App is up" && exit 0
            echo "waiting for app ($code)..."; sleep 10
          done
          echo "Smoke test failed"; exit 1
        '''
      }
    }
  }

  post {
    failure { echo "Build failed. Check the stage above, then Grafana/Loki for the app's logs." }
  }
}
