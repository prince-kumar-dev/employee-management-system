document.addEventListener('DOMContentLoaded', function() {
    loadHeader();
    loadFooter();
    updateLoginStateUI(); // Call this to set initial UI based on stored user info
    setupMobileNavToggle();
});

async function loadHeader() {
    const headerElement = document.getElementById('main-header');
    if (headerElement) {
        try {
            const response = await fetch('_header.html');
            if (!response.ok) {
                throw new Error(`Failed to load _header.html: ${response.status} ${response.statusText}`);
            }
            const headerHtml = await response.text();
            headerElement.innerHTML = headerHtml;
            // After header is loaded, setup its dynamic parts
            setupHeaderEventListeners();
            updateLoginStateUI(); // Ensure UI updates after header content is in place
        } catch (error) {
            console.error('Error loading header:', error);
            headerElement.innerHTML = '<p style="color:red; text-align:center;">Error loading header content.</p>';
        }
    }
}

async function loadFooter() {
    const footerElement = document.getElementById('main-footer');
    if (footerElement) {
        try {
            const response = await fetch('_footer.html');
            if (!response.ok) {
                throw new Error(`Failed to load _footer.html: ${response.status} ${response.statusText}`);
            }
            const footerHtml = await response.text();
            footerElement.innerHTML = footerHtml;
            // After footer is loaded, setup its dynamic parts
            setCurrentYear();
            setupNewsletterForm();
        } catch (error) {
            console.error('Error loading footer:', error);
            footerElement.innerHTML = '<p style="color:red; text-align:center;">Error loading footer content.</p>';
        }
    }
}

function setupHeaderEventListeners() {
    const logoutBtn = document.getElementById('logout-btn-header');
    if (logoutBtn) {
        logoutBtn.addEventListener('click', function(event) {
            event.preventDefault();
            logoutUser();
        });
    }
}

function updateLoginStateUI() {
    const loggedInUser = getLoggedInUser(); // Function to get user from localStorage

    // Get header elements AFTER header is potentially loaded
    // Use a slight delay or a more robust check if elements aren't immediately available
    // For now, we assume they will be there if loadHeader has completed.
    // A better way would be to call this function from within loadHeader's success.
    setTimeout(() => {
        const loginBtnHeader = document.getElementById('login-btn-header');
        const registerBtnHeader = document.getElementById('register-btn-header');
        const userInfoHeader = document.getElementById('user-info-header');
        const usernameHeader = document.getElementById('username-header');
        const dashboardLinkContainer = document.getElementById('dashboard-link-container');
        const dashboardLink = document.getElementById('dashboard-link');

        if (loggedInUser && loggedInUser.token) { // Check for token or a significant user property
            // User is logged in
            if (loginBtnHeader) loginBtnHeader.style.display = 'none';
            if (registerBtnHeader) registerBtnHeader.style.display = 'none';
            if (userInfoHeader) userInfoHeader.style.display = 'flex'; // Or 'block' depending on CSS
            if (usernameHeader) usernameHeader.textContent = `Hi, ${loggedInUser.firstName || 'User'}`;

            if (dashboardLinkContainer && dashboardLink) {
                dashboardLinkContainer.style.display = 'block'; // Or 'inline-block'/'flex'
                if (loggedInUser.role === 'ADMIN') {
                    dashboardLink.href = 'dashboard_admin.html';
                    dashboardLink.textContent = 'Admin Dashboard';
                } else if (loggedInUser.role === 'EMPLOYEE') {
                    dashboardLink.href = 'dashboard_employee.html';
                    dashboardLink.textContent = 'My Dashboard';
                } else {
                     dashboardLinkContainer.style.display = 'none'; // Hide if role is unknown
                }
            }
        } else {
            // User is not logged in
            if (loginBtnHeader) loginBtnHeader.style.display = 'inline-block'; // Or 'block'
            if (registerBtnHeader) registerBtnHeader.style.display = 'inline-block'; // Or 'block'
            if (userInfoHeader) userInfoHeader.style.display = 'none';
            if (dashboardLinkContainer) dashboardLinkContainer.style.display = 'none';
        }
    }, 100); // Small delay to ensure DOM elements from _header.html are loaded
}


function getLoggedInUser() {
    const user = localStorage.getItem('loggedInUser');
    return user ? JSON.parse(user) : null;
}

function storeLoggedInUser(userData, token) {
    // For simplicity, we store the entire user response.
    // In a real app, you'd be more selective and likely just store the token and essential user info.
    // The 'token' here is conceptual as we haven't implemented token-based auth.
    // If not using tokens, user object itself signifies login.
    const userToStore = { ...userData, token: token || 'dummy_token_for_ui_logic' }; // Add a token placeholder
    localStorage.setItem('loggedInUser', JSON.stringify(userToStore));
    updateLoginStateUI();
}

function logoutUser() {
    localStorage.removeItem('loggedInUser');
    // Potentially call a backend logout endpoint if you had session management there
    // For now, just clear local storage and redirect
    updateLoginStateUI();
    window.location.href = 'login.html'; // Redirect to login page
}


function setCurrentYear() {
    const yearSpan = document.getElementById('currentYear');
    if (yearSpan) {
        yearSpan.textContent = new Date().getFullYear();
    }
}

function setupNewsletterForm() {
    const newsletterForm = document.getElementById('newsletterForm');
    if (newsletterForm) {
        newsletterForm.addEventListener('submit', function(event) {
            event.preventDefault();
            const emailInput = newsletterForm.querySelector('input[type="email"]');
            if (emailInput && emailInput.value) {
                alert(`Thank you for subscribing: ${emailInput.value}`);
                emailInput.value = ''; // Clear input
            } else {
                alert('Please enter a valid email address.');
            }
            // In a real app, this would make an API call to a newsletter service
        });
    }
}

function setupMobileNavToggle() {
    // This needs to be called AFTER header is loaded
    setTimeout(() => { // Delay to ensure elements are present
        const toggleButton = document.querySelector('.mobile-nav-toggle');
        const mainNav = document.getElementById('main-nav');

        if (toggleButton && mainNav) {
            toggleButton.addEventListener('click', () => {
                const isExpanded = mainNav.classList.contains('active');
                mainNav.classList.toggle('active');
                toggleButton.setAttribute('aria-expanded', !isExpanded);
                if (!isExpanded) {
                    toggleButton.innerHTML = '<span>×</span>'; // Change to 'X' icon
                } else {
                    toggleButton.innerHTML = '<span>☰</span>'; // Change back to hamburger
                }
            });
        }
    }, 200); // Increased delay slightly
}

// --- Global Loader Functions ---
function showGlobalLoader(message = "Processing...") {
    const loader = document.getElementById('globalLoader');
    if (loader) {
        const loaderText = loader.querySelector('.loader-text');
        if (loaderText) {
            loaderText.textContent = message;
        }
        loader.style.display = 'flex'; // Or 'block' depending on your CSS for centering
    }
}

function hideGlobalLoader() {
    const loader = document.getElementById('globalLoader');
    if (loader) {
        loader.style.display = 'none';
    }
}


// Global utility to make API calls (can be expanded)
async function makeApiCall(url, method = 'GET', body = null, headers = {}, showLoader = true, loaderMessage = "Processing...") {
    // --- LOADER: Show loader if requested ---
    if (showLoader) {
        showGlobalLoader(loaderMessage);
    }

    const defaultHeaders = {
        'Content-Type': 'application/json',
        // 'Authorization': `Bearer ${getLoggedInUser()?.token}`
    };

    const config = {
        method: method,
        headers: { ...defaultHeaders, ...headers },
    };

    if (body && (method === 'POST' || method === 'PUT' || method === 'PATCH')) {
        config.body = JSON.stringify(body);
    }

    try {
        const response = await fetch(url, config);

        if (!response.ok) {
            // Try to get the most specific error message from the response body
            let messageFromBackend = null;
            const contentType = response.headers.get("content-type");

            if (contentType && contentType.includes("application/json")) {
                try {
                    const errorData = await response.json();
                    messageFromBackend = errorData.message || errorData.error || JSON.stringify(errorData);
                } catch (jsonError) {
                    console.warn("makeApiCall: Backend indicated JSON error but failed to parse:", jsonError);
                    try {
                        messageFromBackend = await response.text();
                    } catch (textErrorFallback) {
                        console.warn("makeApiCall: Failed to read error response as text after JSON parse failure.", textErrorFallback);
                    }
                }
            } else {
                try {
                    messageFromBackend = await response.text();
                } catch (textError) {
                    console.warn("makeApiCall: Failed to read non-JSON error response as text.", textError);
                }
            }

            const finalErrorMessage = messageFromBackend || `API Error: ${response.status} ${response.statusText}`;
            throw new Error(finalErrorMessage);
        }

        // Handle successful responses
        const contentType = response.headers.get("content-type");
        if (response.status === 204 || (response.status === 200 && (!contentType || !contentType.includes("application/json")))) {
            // --- LOADER: Hide loader on successful non-JSON or no-content response ---
            // This return happens before the finally block in this specific path,
            // so we need to hide it here OR rely on the finally block (which is better).
            // For simplicity and robustness, the finally block is preferred.
            // No specific hide here, will be caught by finally.
            return null;
        }

        const jsonData = await response.json(); // For successful JSON responses
        // --- LOADER: Hide loader on successful JSON response ---
        // No specific hide here, will be caught by finally.
        return jsonData;

    } catch (error) {
        console.error(`Original error in makeApiCall to ${url} (${method}):`, error);
        if (error instanceof Error) {
            throw error;
        } else {
            throw new Error(String(error) || 'Network request failed or an unknown error occurred.');
        }
    } finally {
        // --- LOADER: Always hide loader after the operation completes (success or error) ---
        if (showLoader) {
            hideGlobalLoader();
        }
    }
}