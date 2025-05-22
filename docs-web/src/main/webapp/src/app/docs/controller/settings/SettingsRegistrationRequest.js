'use strict';

/**
 * 注册请求管理控制器。
 */
angular.module('docs').controller('SettingsRegistrationRequest', function($scope, $state, Restangular, $dialog, $translate) {
  // 初始化分页参数
  $scope.currentPage = 0;
  $scope.limit = 20;
  $scope.offset = 0;
  $scope.search = '';
  $scope.status = 'PENDING'; // 默认显示待处理的请求
  $scope.requests = [];
  $scope.totalPages = 0;
  
  // 加载请求列表
  $scope.loadRequests = function() {
    Restangular.one('registrationrequest').one('list')
      .get({
        limit: $scope.limit,
        offset: $scope.offset,
        search: $scope.search,
        status: $scope.status
      })
      .then(function(data) {
        $scope.requests = data.requests;
        $scope.total = data.total;
        $scope.totalPages = Math.ceil($scope.total / $scope.limit);
      });
  };
  
  // 初始加载
  $scope.loadRequests();
  
  // 搜索变化时刷新
  $scope.$watch('search', function() {
    $scope.currentPage = 0;
    $scope.offset = 0;
    $scope.loadRequests();
  });
  
  // 状态变化时刷新
  $scope.$watch('status', function() {
    $scope.currentPage = 0;
    $scope.offset = 0;
    $scope.loadRequests();
  });
  
  // 每页条数变化时刷新
  $scope.$watch('limit', function() {
    $scope.currentPage = 0;
    $scope.offset = 0;
    $scope.loadRequests();
  });
  
  // 分页方法
  $scope.loadPage = function(page) {
    if (page < 0 || page >= $scope.totalPages) {
      return;
    }
    $scope.currentPage = page;
    $scope.offset = page * $scope.limit;
    $scope.loadRequests();
  };
  
  // 分页辅助方法
  $scope.getPageArray = function() {
    var pages = [];
    var pageCount = Math.min(5, $scope.totalPages);
    var startPage = Math.max(0, Math.min($scope.currentPage - Math.floor(pageCount / 2), $scope.totalPages - pageCount));
    for (var i = 0; i < pageCount; i++) {
      pages.push(startPage + i);
    }
    return pages;
  };
  
  // 审批请求
  $scope.approve = function(request) {
    Restangular.one('registrationrequest', request.id).one('approve').post().then(function() {
      $scope.loadRequests();
    });
  };
  
  // 拒绝请求
  $scope.reject = function(request) {
    var title = $translate.instant('settings.registrationrequest.reject_title');
    var msg = $translate.instant('settings.registrationrequest.reject_message');
    var btns = [
      { result: 'cancel', label: $translate.instant('cancel') },
      { result: 'ok', label: $translate.instant('ok'), cssClass: 'btn-primary' }
    ];
    
    $dialog.messageBox(title, msg, btns, function(result) {
      if (result === 'ok') {
        var notes = '';
        Restangular.one('registrationrequest', request.id).one('reject').post('', { notes: notes }).then(function() {
          $scope.loadRequests();
        });
      }
    });
  };
  
  // 删除请求
  $scope.delete = function(request) {
    var title = $translate.instant('settings.registrationrequest.delete_title');
    var msg = $translate.instant('settings.registrationrequest.delete_message');
    var btns = [
      { result: 'cancel', label: $translate.instant('cancel') },
      { result: 'ok', label: $translate.instant('ok'), cssClass: 'btn-primary' }
    ];
    
    $dialog.messageBox(title, msg, btns, function(result) {
      if (result === 'ok') {
        Restangular.one('registrationrequest', request.id).remove().then(function() {
          $scope.loadRequests();
        });
      }
    });
  };
});