const { defineConfig } = require('@vue/cli-service')
module.exports = defineConfig({
  transpileDependencies: true,
  lintOnSave: false, // 完全禁用ESLint
  devServer: {
    port: 8089,
    hot: false, // 禁用热模块替换
    liveReload: true, // 启用实时重载（完全刷新页面）
    client: {
      overlay: {
        warnings: false,
        errors: true
      },
      logging: 'info',
      progress: true // 启用进度条显示
    },
    historyApiFallback: true, // 解决Vue Router history模式刷新404问题
    proxy: {
        '/api': {
          target: 'http://localhost:8888',
          changeOrigin: true,
          pathRewrite: {
            '^/api': '/api'
          },
          logLevel: 'debug',
          onProxyReq: (proxyReq, req, res) => {
            console.log('代理请求:', req.method, req.url, '->', proxyReq.path)
          },
          onError: (err, req, res) => {
            console.error('代理错误:', err)
          }
        }
      }
  },
  configureWebpack: {
    devtool: 'eval-source-map',
    resolve: {
      fallback: {
        "path": false,
        "fs": false
      }
    }
  },
  chainWebpack: config => {
    config.plugin('define').tap(args => {
      args[0].__VUE_PROD_HYDRATION_MISMATCH_DETAILS__ = JSON.stringify(true)
      return args
    })
  }
})
