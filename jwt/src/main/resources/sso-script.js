const { h, render } = preact;
const { useState } = preactHooks;
const html = htm.bind(h);

function mapAuthGuardErrorCode(errorCode) {
  if (!errorCode) {
    return "Unknown error occurred";
  }

  switch (errorCode) {
    case "AC.033": return "Account deactivated";
    case "AT.032": return "Account is locked";
    case "PW.021":
    case "CD.011":
      return "Either the email or password is incorrect";
    default:
      return `Unknown error occurred: ${errorCode}`;
  }
}

async function post(url, data) {
  const csrfCookie = document.cookie
        .split("; ")
        .find((row) => row.startsWith("CSRF-TOKEN="))
        ?.split("=")[1];
  console.log(document.cookie.split("; "));
  const response = await fetch(url, {
    method: "POST",
    cache: "no-cache",
    redirect: "follow",
    headers: {
      "Content-Type": "application/json",
      "X-CSRF-TOKEN": csrfCookie
    },
    body: JSON.stringify(data)
  });

  if (response.type === "cors" && response.redirected === true) {
    window.location.href = response.url;
    return;
  }

  if (response.status === 302) {
    const redirectUri = response.headers.get("Location");
    if (redirectUri) {
      window.location.href = redirectUri;
      return null;
    } else {
      throw new Error("302 response missing Location header");
    }
  }

  return await response.json();
}


async function login(identifier, password) {
  let path = window.location.pathname.split('/');
  let domain = path[path.indexOf("oidc") + 1];
  let searchParams = new URLSearchParams(window.location.search);
  let clientId = searchParams.get("client_id");
  let redirectUri = searchParams.get("redirect_uri");
  let token = searchParams.get("token");

  let requestBody = {
    identifier: identifier,
    password: password,
    clientId: clientId,
    redirectUri: redirectUri,
    requestToken: token
  };

  return await post("/oidc/" + domain + "/auth", requestBody);
}


function App() {
  const [step, setStep] = useState('login');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [transactionId, setTransactionId] = useState('');
  const [sessionId, setSessionId] = useState('');

  async function handleLogin(e) {
    e.preventDefault();
    setLoading(true);
    setError('');
    const username = e.target.username.value;
    const password = e.target.password.value;

    try {
      const data = await login(username, password);

      if (data.type === 'otp') {
        setTransactionId(data.token);
        setStep('otp');
        setSessionId(data.trackingSession);
      } else if (data.redirectUri) {
        window.location.href = data.redirectUri;
      } else if (data.errorCode) {
        setError(mapAuthGuardErrorCode(data.errorCode));
      } else {
        setError("Unexpected response from server.");
      }
    } catch (err) {
      setError('Login request failed.');
    } finally {
      setLoading(false);
    }
  }


  async function handleOTP(e) {
    e.preventDefault();
    setLoading(true);
    setError('');

    const path = window.location.pathname.split('/');
    const domain = path[path.indexOf("oidc") + 1];
    const searchParams = new URLSearchParams(window.location.search);
    const clientId = searchParams.get("client_id");
    const otp = e.target.otp.value;
    const token = searchParams.get("token");
    const redirectUri = searchParams.get("redirect_uri");

    const requestBody = {
      identifier: transactionId,
      password: otp,
      clientId: clientId,
      redirectUri: redirectUri,
      clientId: clientId,
      requestToken: token,
      trackingSession: sessionId
    };

    return await post("/oidc/" + domain + "/otp", requestBody);
  }

  return html`
    <div class="bg-white dark:bg-gray-800 rounded-2xl shadow-2xl w-full max-w-md p-8 flex flex-col gap-6">
      <h1 class="text-2xl font-semibold text-center text-gray-800 dark:text-white">
        ${step === 'login' ? 'Login' : 'Verify OTP'}
      </h1>

      ${error && html`<div class="text-sm text-red-600 text-center">${error}</div>`}

      ${step === 'login' && html`
        <form onSubmit=${handleLogin} class="flex flex-col gap-4">
          <input name="username" type="text" required placeholder="Username"
            class="px-4 py-3 border border-gray-300 dark:border-gray-600 rounded-md bg-white dark:bg-gray-700 text-black dark:text-white focus:ring-2 focus:ring-green-500" />

          <div class="relative">
            <input name="password" type=${showPassword ? 'text' : 'password'} required placeholder="Password"
              class="w-full px-4 py-3 border border-gray-300 dark:border-gray-600 rounded-md pr-12 bg-white dark:bg-gray-700 text-black dark:text-white focus:ring-2 focus:ring-green-500" />
            <button type="button" onClick=${() => setShowPassword(!showPassword)}
              class="absolute right-3 top-1/2 transform -translate-y-1/2 text-gray-500 dark:text-gray-300">
              ${showPassword ?
        html`<svg xmlns="http://www.w3.org/2000/svg" class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                    d="M13.875 18.825A10.05 10.05 0 0112 19c-4.477 0-8.268-2.943-9.542-7
                       a9.964 9.964 0 012.258-3.743m3.034-2.135
                       A9.956 9.956 0 0112 5c4.477 0 8.268 2.943 9.542 7
                       a9.96 9.96 0 01-4.033 5.147M15 12
                       a3 3 0 00-3-3m0 0a3 3 0 00-3 3m6 0
                       a3 3 0 01-3 3m0 0a3 3 0 01-3-3" />
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                    d="M3 3l18 18" />
                </svg>`: html`<svg xmlns="http://www.w3.org/2000/svg" class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                    d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                    d="M2.458 12C3.732 7.943 7.523 5 12 5
                       c4.477 0 8.268 2.943 9.542 7
                       -1.274 4.057-5.065 7-9.542 7
                       -4.477 0-8.268-2.943-9.542-7z" />
                </svg>`}
            </button>
          </div>

          <button type="submit" class="bg-green-600 text-white rounded-md py-3 font-medium hover:bg-green-700 flex justify-center items-center gap-2" disabled=${loading}>
            <span>${loading ? 'Logging in...' : 'Login'}</span>
            ${loading && html`
              <svg class="animate-spin h-5 w-5 text-white" xmlns="http://www.w3.org/2000/svg" fill="none"
                viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                <path class="opacity-75" fill="currentColor"
                  d="M4 12a8 8 0 018-8v4a4 4 0 00-4 4H4z" />
              </svg>
            `}
          </button>
        </form>
      `}

      ${step === 'otp' && html`
        <form onSubmit=${handleOTP} class="flex flex-col gap-4">
          <input name="otp" type="text" required placeholder="Enter OTP"
            class="px-4 py-3 border border-gray-300 dark:border-gray-600 rounded-md bg-white dark:bg-gray-700 text-black dark:text-white focus:ring-2 focus:ring-green-500" />

          <button type="submit" class="bg-green-600 text-white rounded-md py-3 font-medium hover:bg-green-700 flex justify-center items-center gap-2" disabled=${loading}>
            <span>${loading ? 'Verifying...' : 'Verify OTP'}</span>
            ${loading && html`
              <svg class="animate-spin h-5 w-5 text-white" xmlns="http://www.w3.org/2000/svg" fill="none"
                viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                <path class="opacity-75" fill="currentColor"
                  d="M4 12a8 8 0 018-8v4a4 4 0 00-4 4H4z" />
              </svg>
            `}
          </button>
        </form>
      `}

      <div class="text-center mt-8 text-base">
        <span class="text-black dark:text-white">Powered by </span>
        <span class="text-[#4CAF50] font-bold">AuthGuard</span>
      </div>
    </div>
  `;
}

render(h(App), document.getElementById('app'));