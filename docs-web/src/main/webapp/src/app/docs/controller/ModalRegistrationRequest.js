'use strict';

/**
 * 注册请求模态窗口控制器。
 */
angular.module('docs').controller('ModalRegistrationRequest', function($scope, $uibModalInstance, Restangular, $translate, $timeout) {
  $scope.user = {
    username: '',
    password: '',
    email: ''
  };
  
  $scope.submit = function() {
    if (!$scope.user.username || !$scope.user.password || !$scope.user.email) {
      return;
    }
    
    Restangular.one('registrationrequest').put($scope.user).then(function() {
      $scope.registrationSuccess = true;
      $timeout(function() {
        $uibModalInstance.close();
      }, 2000);
    }, function(data) {
      $scope.registrationError = data.data.message;
    });
  };
  
  $scope.close = function() {
    $uibModalInstance.dismiss('cancel');
  };
});