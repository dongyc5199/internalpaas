package middleware

import (
	"net/http"
	"strings"

	"github.com/gin-gonic/gin"
	"go.uber.org/zap"

	"github.com/yourorg/codehub/internal/pkg/jwt"
)

// AuthMiddleware JWT认证中间件
func AuthMiddleware(logger *zap.Logger) gin.HandlerFunc {
	return func(c *gin.Context) {
		// 从请求头获取token
		authHeader := c.GetHeader("Authorization")
		if authHeader == "" {
			c.JSON(http.StatusUnauthorized, gin.H{
				"error": "未提供认证token",
			})
			c.Abort()
			return
		}

		// 验证Bearer格式
		parts := strings.SplitN(authHeader, " ", 2)
		if len(parts) != 2 || parts[0] != "Bearer" {
			c.JSON(http.StatusUnauthorized, gin.H{
				"error": "认证格式错误，应为: Bearer {token}",
			})
			c.Abort()
			return
		}

		tokenString := parts[1]

		// 解析token
		claims, err := jwt.ParseToken(tokenString)
		if err != nil {
			logger.Warn("Invalid token",
				zap.Error(err),
				zap.String("ip", c.ClientIP()),
			)

			c.JSON(http.StatusUnauthorized, gin.H{
				"error": err.Error(),
			})
			c.Abort()
			return
		}

		// 将用户信息存入上下文
		c.Set("user_id", claims.UserID)
		c.Set("username", claims.Username)
		c.Set("email", claims.Email)
		c.Set("is_admin", claims.IsAdmin)

		logger.Debug("Authenticated request",
			zap.Uint("user_id", claims.UserID),
			zap.String("username", claims.Username),
			zap.String("path", c.Request.URL.Path),
		)

		c.Next()
	}
}

// AdminMiddleware 管理员权限中间件（需要在AuthMiddleware之后使用）
func AdminMiddleware() gin.HandlerFunc {
	return func(c *gin.Context) {
		isAdmin, exists := c.Get("is_admin")
		if !exists || !isAdmin.(bool) {
			c.JSON(http.StatusForbidden, gin.H{
				"error": "需要管理员权限",
			})
			c.Abort()
			return
		}

		c.Next()
	}
}

// GetCurrentUserID 从上下文获取当前用户ID
func GetCurrentUserID(c *gin.Context) (uint, bool) {
	userID, exists := c.Get("user_id")
	if !exists {
		return 0, false
	}
	return userID.(uint), true
}

// GetCurrentUsername 从上下文获取当前用户名
func GetCurrentUsername(c *gin.Context) (string, bool) {
	username, exists := c.Get("username")
	if !exists {
		return "", false
	}
	return username.(string), true
}

// IsAdmin 判断当前用户是否为管理员
func IsAdmin(c *gin.Context) bool {
	isAdmin, exists := c.Get("is_admin")
	if !exists {
		return false
	}
	return isAdmin.(bool)
}
