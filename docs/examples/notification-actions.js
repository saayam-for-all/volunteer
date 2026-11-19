/** Copy into the frontend's existing API layer; apiBaseUrl includes any deployed stage prefix. */
export async function notificationAction({ apiBaseUrl, notificationId, userId, accessToken, action }) {
  if (!['accept', 'deny'].includes(action)) throw new Error('Invalid notification action');
  const base = apiBaseUrl.replace(/\/$/, '');
  const response = await fetch(
    `${base}/0.0.1/notifications/${encodeURIComponent(notificationId)}/${action}`,
    {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${accessToken}`,
        'X-User-Id': userId,
      },
    },
  );
  const body = await response.json().catch(() => null);
  if (response.status === 409 && body?.saayamCode === 'SAAAYAM-1323') {
    return { outcome: 'request_taken', data: body.data, message: body.message };
  }
  if (!response.ok || body?.success !== true) {
    const error = new Error(body?.message || `Notification action failed (${response.status})`);
    error.status = response.status;
    error.code = body?.saayamCode;
    throw error;
  }
  return { outcome: 'completed', data: body.data, message: body.message };
}
