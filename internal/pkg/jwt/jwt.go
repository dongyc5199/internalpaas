package jwt

import (
	"errors"
	"time"

	"github.com/golang-jwt/jwt/v5"
	"github.com/spf13/viper"
)

var (
	ErrTokenExpired     = errors.New("token已过期")
	ErrTokenInvalid     = errors.New("token无效")
	ErrTokenNotValidYet = errors.New("token尚未生效")
)

// Claims JWT声明
type Claims struct {
	UserID   uint   `json:"user_id"`
	Username string `json:"username"`
	Email    string `json:"email"`
	IsAdmin  bool   `json:"is_admin"`
	jwt.RegisteredClaims
}

// GenerateToken 生成JWT token
func GenerateToken(userID uint, username, email string, isAdmin bool) (string, error) {
	secret := viper.GetString("jwt.secret")
	if secret == "" {
		return "", errors.New("jwt.secret not configured")
	}

	expireHours := viper.GetInt("jwt.expire_hours")
	if expireHours == 0 {
		expireHours = 24
	}

	issuer := viper.GetString("jwt.issuer")
	if issuer == "" {
		issuer = "codehub"
	}

	now := time.Now()
	claims := Claims{
		UserID:   userID,
		Username: username,
		Email:    email,
		IsAdmin:  isAdmin,
		RegisteredClaims: jwt.RegisteredClaims{
			Issuer:    issuer,
			Subject:   username,
			IssuedAt:  jwt.NewNumericDate(now),
			ExpiresAt: jwt.NewNumericDate(now.Add(time.Duration(expireHours) * time.Hour)),
			NotBefore: jwt.NewNumericDate(now),
		},
	}

	token := jwt.NewWithClaims(jwt.SigningMethodHS256, claims)
	return token.SignedString([]byte(secret))
}

// ParseToken 解析JWT token
func ParseToken(tokenString string) (*Claims, error) {
	secret := viper.GetString("jwt.secret")
	if secret == "" {
		return nil, errors.New("jwt.secret not configured")
	}

	token, err := jwt.ParseWithClaims(tokenString, &Claims{}, func(token *jwt.Token) (interface{}, error) {
		return []byte(secret), nil
	})

	if err != nil {
		if errors.Is(err, jwt.ErrTokenExpired) {
			return nil, ErrTokenExpired
		}
		if errors.Is(err, jwt.ErrTokenNotValidYet) {
			return nil, ErrTokenNotValidYet
		}
		return nil, ErrTokenInvalid
	}

	if claims, ok := token.Claims.(*Claims); ok && token.Valid {
		return claims, nil
	}

	return nil, ErrTokenInvalid
}

// RefreshToken 刷新token
func RefreshToken(tokenString string) (string, error) {
	claims, err := ParseToken(tokenString)
	if err != nil {
		// 即使过期也允许刷新（7天内）
		if !errors.Is(err, ErrTokenExpired) {
			return "", err
		}

		// 检查是否在刷新窗口内（7天）
		if time.Since(claims.ExpiresAt.Time) > 7*24*time.Hour {
			return "", errors.New("token过期时间过长，请重新登录")
		}
	}

	// 生成新token
	return GenerateToken(claims.UserID, claims.Username, claims.Email, claims.IsAdmin)
}
