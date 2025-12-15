package v1

import (
	"net/http"
	"time"

	"github.com/gin-gonic/gin"
	"go.uber.org/zap"
	"golang.org/x/crypto/bcrypt"
	"gorm.io/gorm"

	"github.com/yourorg/codehub/internal/model"
	"github.com/yourorg/codehub/internal/pkg/jwt"
)

// AuthHandler 认证处理器
type AuthHandler struct {
	db     *gorm.DB
	logger *zap.Logger
}

// NewAuthHandler 创建认证处理器
func NewAuthHandler(db *gorm.DB, logger *zap.Logger) *AuthHandler {
	return &AuthHandler{
		db:     db,
		logger: logger,
	}
}

// RegisterRequest 注册请求
type RegisterRequest struct {
	Username string `json:"username" binding:"required,min=3,max=50"`
	Email    string `json:"email" binding:"required,email"`
	Password string `json:"password" binding:"required,min=6"`
	FullName string `json:"full_name"`
}

// LoginRequest 登录请求
type LoginRequest struct {
	Username string `json:"username" binding:"required"`
	Password string `json:"password" binding:"required"`
}

// LoginResponse 登录响应
type LoginResponse struct {
	Token     string      `json:"token"`
	ExpiresAt int64       `json:"expires_at"`
	User      *model.User `json:"user"`
}

// Register 用户注册
// @Summary 用户注册
// @Tags auth
// @Accept json
// @Produce json
// @Param request body RegisterRequest true "注册信息"
// @Success 200 {object} model.User
// @Router /api/v1/auth/register [post]
func (h *AuthHandler) Register(c *gin.Context) {
	var req RegisterRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{
			"error": "请求参数错误: " + err.Error(),
		})
		return
	}

	// 检查用户名是否已存在
	var existingUser model.User
	if err := h.db.Where("username = ?", req.Username).First(&existingUser).Error; err == nil {
		c.JSON(http.StatusConflict, gin.H{
			"error": "用户名已存在",
		})
		return
	}

	// 检查邮箱是否已存在
	if err := h.db.Where("email = ?", req.Email).First(&existingUser).Error; err == nil {
		c.JSON(http.StatusConflict, gin.H{
			"error": "邮箱已被使用",
		})
		return
	}

	// 加密密码
	hashedPassword, err := bcrypt.GenerateFromPassword([]byte(req.Password), bcrypt.DefaultCost)
	if err != nil {
		h.logger.Error("Failed to hash password", zap.Error(err))
		c.JSON(http.StatusInternalServerError, gin.H{
			"error": "密码加密失败",
		})
		return
	}

	// 创建用户
	user := model.User{
		Username: req.Username,
		Email:    req.Email,
		Password: string(hashedPassword),
		FullName: req.FullName,
		IsActive: true,
		IsAdmin:  false,
	}

	if err := h.db.Create(&user).Error; err != nil {
		h.logger.Error("Failed to create user", zap.Error(err))
		c.JSON(http.StatusInternalServerError, gin.H{
			"error": "创建用户失败",
		})
		return
	}

	h.logger.Info("User registered",
		zap.Uint("user_id", user.ID),
		zap.String("username", user.Username),
		zap.String("ip", c.ClientIP()),
	)

	// 清除密码字段
	user.Password = ""

	c.JSON(http.StatusOK, gin.H{
		"message": "注册成功",
		"user":    user,
	})
}

// Login 用户登录
// @Summary 用户登录
// @Tags auth
// @Accept json
// @Produce json
// @Param request body LoginRequest true "登录信息"
// @Success 200 {object} LoginResponse
// @Router /api/v1/auth/login [post]
func (h *AuthHandler) Login(c *gin.Context) {
	var req LoginRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{
			"error": "请求参数错误: " + err.Error(),
		})
		return
	}

	// 查找用户
	var user model.User
	if err := h.db.Where("username = ?", req.Username).First(&user).Error; err != nil {
		c.JSON(http.StatusUnauthorized, gin.H{
			"error": "用户名或密码错误",
		})
		return
	}

	// 检查用户是否激活
	if !user.IsActive {
		c.JSON(http.StatusForbidden, gin.H{
			"error": "账户已被禁用",
		})
		return
	}

	// 验证密码
	if err := bcrypt.CompareHashAndPassword([]byte(user.Password), []byte(req.Password)); err != nil {
		c.JSON(http.StatusUnauthorized, gin.H{
			"error": "用户名或密码错误",
		})
		return
	}

	// 生成JWT token
	token, err := jwt.GenerateToken(user.ID, user.Username, user.Email, user.IsAdmin)
	if err != nil {
		h.logger.Error("Failed to generate token", zap.Error(err))
		c.JSON(http.StatusInternalServerError, gin.H{
			"error": "生成token失败",
		})
		return
	}

	// 更新最后登录信息
	now := time.Now()
	user.LastLoginAt = &now
	user.LastLoginIP = c.ClientIP()
	h.db.Save(&user)

	h.logger.Info("User logged in",
		zap.Uint("user_id", user.ID),
		zap.String("username", user.Username),
		zap.String("ip", c.ClientIP()),
	)

	// 清除密码字段
	user.Password = ""

	// 计算过期时间
	expiresAt := time.Now().Add(24 * time.Hour).Unix()

	c.JSON(http.StatusOK, LoginResponse{
		Token:     token,
		ExpiresAt: expiresAt,
		User:      &user,
	})
}

// RefreshToken 刷新token
// @Summary 刷新token
// @Tags auth
// @Security BearerAuth
// @Produce json
// @Success 200 {object} LoginResponse
// @Router /api/v1/auth/refresh [post]
func (h *AuthHandler) RefreshToken(c *gin.Context) {
	// 从请求头获取当前token
	authHeader := c.GetHeader("Authorization")
	if authHeader == "" {
		c.JSON(http.StatusUnauthorized, gin.H{
			"error": "未提供认证token",
		})
		return
	}

	tokenString := authHeader[7:] // 去掉 "Bearer "

	// 刷新token
	newToken, err := jwt.RefreshToken(tokenString)
	if err != nil {
		c.JSON(http.StatusUnauthorized, gin.H{
			"error": err.Error(),
		})
		return
	}

	// 解析新token获取用户信息
	claims, _ := jwt.ParseToken(newToken)

	// 查询用户信息
	var user model.User
	if err := h.db.First(&user, claims.UserID).Error; err != nil {
		c.JSON(http.StatusNotFound, gin.H{
			"error": "用户不存在",
		})
		return
	}

	// 清除密码字段
	user.Password = ""

	// 计算过期时间
	expiresAt := time.Now().Add(24 * time.Hour).Unix()

	c.JSON(http.StatusOK, LoginResponse{
		Token:     newToken,
		ExpiresAt: expiresAt,
		User:      &user,
	})
}

// GetCurrentUser 获取当前登录用户信息
// @Summary 获取当前用户
// @Tags auth
// @Security BearerAuth
// @Produce json
// @Success 200 {object} model.User
// @Router /api/v1/auth/me [get]
func (h *AuthHandler) GetCurrentUser(c *gin.Context) {
	userID, exists := c.Get("user_id")
	if !exists {
		c.JSON(http.StatusUnauthorized, gin.H{
			"error": "未认证",
		})
		return
	}

	var user model.User
	if err := h.db.First(&user, userID).Error; err != nil {
		c.JSON(http.StatusNotFound, gin.H{
			"error": "用户不存在",
		})
		return
	}

	// 清除密码字段
	user.Password = ""

	c.JSON(http.StatusOK, user)
}
