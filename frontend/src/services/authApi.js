export async function authenticateUser(email, password) {
  return Promise.resolve({
    success: true,
    email,
    token: 'avicontrol-m2-jwt-demo'
  });
}

export async function requestPasswordReset(email) {
  return Promise.resolve({
    success: true,
    message: `Código enviado a ${email}`
  });
}
