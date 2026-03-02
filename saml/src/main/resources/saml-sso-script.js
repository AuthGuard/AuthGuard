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

function getCookie(name) {
  const cookie = document.cookie
    .split("; ")
    .find((row) => row.startsWith(name + "="));
  return cookie ? cookie.split("=")[1] : null;
}

async function post(url, data) {
  const csrfCookie = getCookie("CSRF-TOKEN");

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

  const ct = response.headers.get("content-type") || "";

  if (ct.includes("text/html")) {
    const html = await response.text();

    // Hand off to the browser so the onload auto-submit runs
    const blob = new Blob([html], { type: "text/html" });
    const urlObject = URL.createObjectURL(blob);
    window.location.replace(urlObject);
    return { type: "html_navigation_started" };
  }

  return await response.json();
}


async function checkSession() {
  const astCookie = getCookie("AST");
  if (!astCookie) {
    return { hasSession: false };
  }

  const path = window.location.pathname.split('/');
  const domain = path[path.indexOf("saml") + 1];
  const searchParams = new URLSearchParams(window.location.search);
  const token = searchParams.get("token");

  const requestBody = {
    requestToken: token
  };

  try {
    const response = await post("/saml/" + domain + "/session", requestBody);
    return { hasSession: true, response: response };
  } catch (err) {
    return { hasSession: false, error: err };
  }
}

async function login(identifier, password) {
  let path = window.location.pathname.split('/');
  let domain = path[path.indexOf("saml") + 1];
  let searchParams = new URLSearchParams(window.location.search);
  let clientId = searchParams.get("client_id");
  let redirectUri = searchParams.get("redirect_uri");
  let token = searchParams.get("token");

  let requestBody = {
    identifier: identifier,
    password: password,
    requestToken: token
  };

  return await post("/saml/" + domain + "/auth", requestBody);
}


function App() {
  const [step, setStep] = useState('login');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [transactionId, setTransactionId] = useState('');
  const [sessionId, setSessionId] = useState('');
  const [checkingSession, setCheckingSession] = useState(true);

  // Check for existing session on mount
  preactHooks.useEffect(() => {
    async function performSessionCheck() {
      const result = await checkSession();
      
      if (result.hasSession && result.response) {
        // If response is html_navigation_started, the redirect is already happening
        if (result.response.type === 'html_navigation_started') {
          return;
        }
        // Otherwise show the login form
      }
      
      setCheckingSession(false);
    }
    
    performSessionCheck();
  }, []);

  async function handleLogin(e) {
    e.preventDefault();
    setLoading(true);
    setError('');
    const username = e.target.username.value;
    const password = e.target.password.value;

    try {
      const data = await login(username, password);

      if (data.type === 'html_navigation_started') {
        return;
      }

      if (data.type === 'otp') {
        setTransactionId(data.token);
        setStep('otp');
        setSessionId(data.trackingSession);
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
    const domain = path[path.indexOf("saml") + 1];
    const searchParams = new URLSearchParams(window.location.search);
    const clientId = searchParams.get("client_id");
    const otp = e.target.otp.value;
    const token = searchParams.get("token");
    const redirectUri = searchParams.get("redirect_uri");

    const requestBody = {
      identifier: transactionId,
      password: otp,
      clientId: clientId,
      requestToken: token,
      trackingSession: sessionId
    };

    return await post("/saml/" + domain + "/otp", requestBody);
  }

  return html`
    <div class="bg-white rounded-xl shadow-2xl w-full p-10 flex flex-col gap-6">
      ${checkingSession ? html`
        <div class="flex flex-col items-center justify-center gap-4 py-8">
          <svg class="animate-spin h-10 w-10 text-green-600" xmlns="http://www.w3.org/2000/svg" fill="none"
            viewBox="0 0 24 24">
            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
            <path class="opacity-75" fill="currentColor"
              d="M4 12a8 8 0 018-8v4a4 4 0 00-4 4H4z" />
          </svg>
          <p class="text-gray-600">Checking session...</p>
        </div>
      ` : html`
        <h1 class="text-2xl font-semibold text-gray-800 text-center">
          ${step === 'login' ? 'Sign in' : 'Verify OTP'}
        </h1>

        ${error && html`<div class="text-sm text-red-600 bg-red-50 p-3 rounded-md">${error}</div>`}

      ${step === 'login' && html`
        <form onSubmit=${handleLogin} class="flex flex-col gap-5">
          <div class="flex flex-col gap-2">
            <label for="username" class="text-sm font-medium text-gray-700">Username</label>
            <input 
              id="username"
              name="username" 
              type="text"
              required 
              class="px-4 py-3 border border-gray-300 rounded-md bg-white text-gray-900 focus:outline-none focus:ring-2 focus:ring-green-400 focus:border-transparent" 
            />
          </div>

          <!-- Password Field -->
          <div class="flex flex-col gap-2">
            <div class="flex justify-between items-center">
              <label for="password" class="text-sm font-medium text-gray-700">Password</label>
            </div>
            <input 
              id="password"
              name="password" 
              type="password" 
              required 
              class="px-4 py-3 border border-gray-300 rounded-md bg-white text-gray-900 focus:outline-none focus:ring-2 focus:ring-green-400 focus:border-transparent" 
            />
          </div>

//          <div class="flex items-center gap-2">
//            <input
//              type="checkbox"
//              id="remember"
//              class="w-4 h-4 text-green-600 border-gray-300 rounded focus:ring-green-500"
//            />
//            <label for="remember" class="text-sm text-gray-700">Remember me on this device</label>
//          </div>

          <button 
            type="submit" 
            class="bg-green-600 text-white rounded-md py-3 font-medium hover:bg-green-700 transition-colors flex justify-center items-center gap-2" 
            disabled=${loading}
          >
            <span>${loading ? 'Signing in...' : 'Sign in'}</span>
            ${loading && html`
              <svg class="animate-spin h-5 w-5 text-white" xmlns="http://www.w3.org/2000/svg" fill="none"
                viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                <path class="opacity-75" fill="currentColor"
                  d="M4 12a8 8 0 018-8v4a4 0 00-4 4H4z" />
              </svg>
            `}
          </button>
        </form>

        <div class="text-center mt-4 text-base">
          <span class="text-gray-700">Powered by </span>
          <span class="text-[#4CAF50] font-bold">AuthGuard</span>
        </div>
      `}

      ${step === 'otp' && html`
        <form onSubmit=${handleOTP} class="flex flex-col gap-4">
          <input name="otp" type="text" required placeholder="Enter OTP"
            class="px-4 py-3 border border-gray-300 rounded-md bg-white text-gray-900 focus:outline-none focus:ring-2 focus:ring-green-400 focus:border-transparent" />

          <button type="submit" class="bg-green-600 text-white rounded-md py-3 font-medium hover:bg-green-700 transition-colors flex justify-center items-center gap-2" disabled=${loading}>
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
      `}
    </div>
  `;
}

render(h(App), document.getElementById('app'));