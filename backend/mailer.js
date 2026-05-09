const { Resend } = require('resend');

const resend = new Resend(process.env.RESEND_API_KEY);

/**
 * 寄送 Email 驗證信
 * @param {string} toEmail   收件者 Email
 * @param {string} username  使用者名稱
 * @param {string} token     驗證 token
 */
async function sendVerificationEmail(toEmail, username, token) {
  const baseUrl = process.env.BASE_URL || `http://localhost:${process.env.PORT || 3000}`;
  const verifyUrl = `${baseUrl}/api/auth/verify-email?token=${token}`;

  await resend.emails.send({
    from: 'onboarding@resend.dev',   // 免費帳號用此寄件地址
    to: toEmail,
    subject: '【環保餐具借還系統】請驗證您的電子郵件',
    html: `
      <div style="font-family: Arial, sans-serif; max-width: 480px; margin: 0 auto;">
        <h2 style="color: #4CAF50;">環保餐具借還系統</h2>
        <p>您好，<strong>${username}</strong>！</p>
        <p>感謝您的註冊。請點擊下方按鈕完成電子郵件驗證，驗證後即可登入使用。</p>
        <div style="text-align: center; margin: 32px 0;">
          <a href="${verifyUrl}"
             style="background-color: #4CAF50; color: white; padding: 14px 28px;
                    text-decoration: none; border-radius: 6px; font-size: 16px;">
            驗證電子郵件
          </a>
        </div>
        <p style="color: #888; font-size: 12px;">
          此連結將於 24 小時後失效。<br>
          若非本人操作，請忽略此郵件。
        </p>
        <hr style="border: none; border-top: 1px solid #eee;">
        <p style="color: #aaa; font-size: 11px;">環保餐具借還系統 — 讓地球更美好 🌍</p>
      </div>
    `,
  });
}

module.exports = { sendVerificationEmail };
