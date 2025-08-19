/**
 * 浏览器兼容性检测脚本
 * 在应用加载前运行，检测可能导致白屏的兼容性问题
 */
(function() {
  'use strict';
  
  // 创建兼容性检测日志
  const compatibilityLog = [];
  
  function log(message, level = 'info') {
    compatibilityLog.push({ message, level, timestamp: new Date().toISOString() });
    if (level === 'error') {
      console.error('[兼容性检测]', message);
    } else {
      console.log('[兼容性检测]', message);
    }
  }
  
  // 检测基础特性支持
  function checkBasicFeatures() {
    const features = {
      'Promise': typeof Promise !== 'undefined',
      'fetch': typeof fetch !== 'undefined',
      'localStorage': (function() {
        try {
          const test = 'test';
          localStorage.setItem(test, test);
          localStorage.removeItem(test);
          return true;
        } catch (e) {
          return false;
        }
      })(),
      'sessionStorage': (function() {
        try {
          const test = 'test';
          sessionStorage.setItem(test, test);
          sessionStorage.removeItem(test);
          return true;
        } catch (e) {
          return false;
        }
      })(),
      'CSS.supports': typeof CSS !== 'undefined' && CSS.supports,
      'requestAnimationFrame': typeof requestAnimationFrame !== 'undefined',
      'Intl': typeof Intl !== 'undefined',
      'Proxy': typeof Proxy !== 'undefined',
      'Reflect': typeof Reflect !== 'undefined',
      'Map': typeof Map !== 'undefined',
      'Set': typeof Set !== 'undefined',
      'Symbol': typeof Symbol !== 'undefined',
      'WeakMap': typeof WeakMap !== 'undefined',
      'WeakSet': typeof WeakSet !== 'undefined'
    };
    
    const missingFeatures = Object.keys(features).filter(key => !features[key]);
    
    if (missingFeatures.length > 0) {
      log(`缺失的特性: ${missingFeatures.join(', ')}`, 'error');
      return false;
    }
    
    log('所有基础特性支持正常');
    return true;
  }
  
  // 检测ES6+特性
  function checkES6Features() {
    try {
      // 检测箭头函数
      new Function('() => {}')();
      
      // 检测解构赋值
      new Function('const {a} = {a: 1}')();
      
      // 检测模板字符串
      new Function('`template ${1}`')();
      
      // 检测类
      new Function('class Test {}')();
      
      // 检测async/await
      new Function('async function test() { await 1 }')();
      
      log('ES6+特性支持正常');
      return true;
    } catch (e) {
      log(`ES6+特性支持异常: ${e.message}`, 'error');
      return false;
    }
  }
  
  // 检测浏览器版本
  function checkBrowserVersion() {
    const ua = navigator.userAgent;
    let browser = 'Unknown';
    let version = 'Unknown';
    
    if (ua.includes('Chrome/')) {
      browser = 'Chrome';
      version = ua.match(/Chrome\/(\d+)/)[1];
    } else if (ua.includes('Firefox/')) {
      browser = 'Firefox';
      version = ua.match(/Firefox\/(\d+)/)[1];
    } else if (ua.includes('Safari/') && !ua.includes('Chrome/')) {
      browser = 'Safari';
      version = ua.match(/Version\/(\d+)/)[1];
    } else if (ua.includes('Edge/')) {
      browser = 'Edge';
      version = ua.match(/Edge\/(\d+)/)[1];
    }
    
    log(`浏览器: ${browser} ${version}`);
    
    // 检查版本兼容性
    const minVersions = {
      'Chrome': 60,
      'Firefox': 55,
      'Safari': 12,
      'Edge': 79
    };
    
    if (minVersions[browser] && parseInt(version) < minVersions[browser]) {
      log(`浏览器版本过低，建议升级至 ${browser} ${minVersions[browser]}+`, 'error');
      return false;
    }
    
    return true;
  }
  
  // 检测网络问题
  function checkNetworkIssues() {
    // 检测是否离线
    if (navigator.onLine === false) {
      log('检测到离线状态', 'error');
      return false;
    }
    
    // 检测可能的网络错误
    if (window.location.protocol === 'file:') {
      log('检测到文件协议访问，可能导致资源加载失败', 'error');
      return false;
    }
    
    log('网络状态正常');
    return true;
  }
  
  // 检测CSS兼容性
  function checkCSSCompatibility() {
    const testElement = document.createElement('div');
    const testStyles = {
      'display': 'flex',
      'transform': 'translate3d(0,0,0)',
      'transition': 'all 0.3s ease',
      'backdrop-filter': 'blur(10px)'
    };
    
    let hasIssues = false;
    Object.keys(testStyles).forEach(property => {
      const value = testStyles[property];
      testElement.style[property] = value;
      if (testElement.style[property] !== value) {
        log(`CSS特性不支持: ${property}`, 'warn');
        hasIssues = true;
      }
    });
    
    if (!hasIssues) {
      log('CSS兼容性正常');
    }
    
    return !hasIssues;
  }
  
  // 检测可能的脚本错误
  function checkScriptErrors() {
    let hasErrors = false;
    
    // 监听全局错误
    window.addEventListener('error', function(event) {
      log(`脚本错误: ${event.message} (${event.filename}:${event.lineno})`, 'error');
      hasErrors = true;
    });
    
    window.addEventListener('unhandledrejection', function(event) {
      log(`未处理的Promise拒绝: ${event.reason}`, 'error');
      hasErrors = true;
    });
    
    return !hasErrors;
  }
  
  // 创建兼容性警告UI
  function createCompatibilityWarning(message) {
    const warning = document.createElement('div');
    warning.id = 'compatibility-warning';
    warning.style.cssText = `
      position: fixed;
      top: 0;
      left: 0;
      width: 100%;
      background: #f56c6c;
      color: white;
      padding: 15px;
      text-align: center;
      z-index: 10000;
      font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', sans-serif;
      font-size: 14px;
      line-height: 1.5;
    `;
    
    warning.innerHTML = `
      <div>
        <strong>⚠️ 浏览器兼容性问题</strong><br>
        ${message}<br>
        <button onclick="location.reload()" style="
          background: rgba(255,255,255,0.2);
          border: 1px solid white;
          color: white;
          padding: 5px 10px;
          border-radius: 3px;
          margin: 5px;
          cursor: pointer;
        ">重试</button>
        <button onclick="this.parentElement.parentElement.remove()" style="
          background: rgba(255,255,255,0.2);
          border: 1px solid white;
          color: white;
          padding: 5px 10px;
          border-radius: 3px;
          margin: 5px;
          cursor: pointer;
        ">忽略</button>
      </div>
    `;
    
    document.body.appendChild(warning);
  }
  
  // 主检测函数
  function runCompatibilityCheck() {
    console.log('开始浏览器兼容性检测...');
    
    const checks = [
      checkBasicFeatures(),
      checkES6Features(),
      checkBrowserVersion(),
      checkNetworkIssues(),
      checkCSSCompatibility(),
      checkScriptErrors()
    ];
    
    const passedChecks = checks.filter(Boolean).length;
    const totalChecks = checks.length;
    
    log(`兼容性检测完成: ${passedChecks}/${totalChecks} 项通过`);
    
    // 如果检测失败，显示警告
    if (passedChecks < totalChecks) {
      const issues = compatibilityLog.filter(item => item.level === 'error').map(item => item.message);
      if (issues.length > 0) {
        setTimeout(() => {
          createCompatibilityWarning(issues[0]);
        }, 1000);
      }
    }
    
    // 保存检测结果到全局
    window.__compatibilityCheck = {
      passed: passedChecks === totalChecks,
      log: compatibilityLog,
      timestamp: new Date().toISOString()
    };
    
    console.log('兼容性检测结果:', window.__compatibilityCheck);
  }
  
  // 延迟执行检测，确保DOM加载完成
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', runCompatibilityCheck);
  } else {
    runCompatibilityCheck();
  }
  
})();