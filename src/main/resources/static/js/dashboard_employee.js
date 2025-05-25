// Ensure common.js is loaded, especially getLoggedInUser
// This script assumes Chart is available globally from chart.min.js

document.addEventListener('DOMContentLoaded', function() {
    // Check if user is logged in and is an EMPLOYEE
    const loggedInUser = getLoggedInUser(); // From common.js

    if (!loggedInUser || loggedInUser.role !== 'EMPLOYEE') {
        // If not employee or not logged in, redirect to login or home
        alert('Access Denied. You must be an Employee to view this page.');
        window.location.href = 'login.html';
        return; // Stop further execution
    }

    populateEmployeeDashboard(loggedInUser);
    setupEventListeners(loggedInUser);
    // loadAnnouncements(); // Optional: if you implement announcements
});

function populateEmployeeDashboard(userData) {
    const employeeNameEl = document.getElementById('employeeName');
    const employeeEmailEl = document.getElementById('employeeEmail');
    const employeeDepartmentEl = document.getElementById('employeeDepartment');
    const employeeRoleEl = document.getElementById('employeeRole');
    const employeeHireDateEl = document.getElementById('employeeHireDate');
    const employeeDateOfBirth = document.getElementById('employeeDateOfBirth');
    const employeeSalary = document.getElementById('employeeSalary');

    // For debugging:
    console.log("User data received in populateEmployeeDashboard:", userData);
    console.log("Hire date raw value:", userData.hireDate);
    console.log("Type of hire date:", typeof userData.hireDate);


    if (employeeNameEl) {
        employeeNameEl.textContent = userData.firstName ? `${userData.firstName} ${userData.lastName || ''}`.trim() : 'Employee';
    }
    if (employeeEmailEl) {
        employeeEmailEl.textContent = userData.email || '-';
    }
    if (employeeDepartmentEl) {
        employeeDepartmentEl.textContent = userData.departmentName || 'N/A';
    }
    if (employeeRoleEl) {
        employeeRoleEl.textContent = userData.role || '-';
    }

    if (employeeHireDateEl) {
        if (userData.hireDate && userData.hireDate !== null) { // Explicitly check for null
            try {
                let formattedHireDate = '-'; // Default to '-' if formatting fails
                let dateToFormat;

                if (Array.isArray(userData.hireDate) && userData.hireDate.length >= 3) {
                    // Assuming [year, month, dayOfMonth, ...]
                    // JavaScript Date months are 0-indexed
                    dateToFormat = new Date(userData.hireDate[0], userData.hireDate[1] - 1, userData.hireDate[2]);
                } else if (typeof userData.hireDate === 'string') {
                    const dateStr = userData.hireDate.split('T')[0]; // Get "YYYY-MM-DD" part
                    const parts = dateStr.split('-');
                    if (parts.length === 3) {
                        // Ensure parts are numbers; month is 1-indexed from backend, convert to 0-indexed for JS Date
                        dateToFormat = new Date(Number(parts[0]), Number(parts[1]) - 1, Number(parts[2]));
                    } else {
                        // Fallback for other string formats, might be risky
                        dateToFormat = new Date(userData.hireDate);
                    }
                }

                // Check if dateToFormat is a valid Date object
                if (dateToFormat && !isNaN(dateToFormat.getTime())) {
                    // Use toLocaleDateString for user-friendly format
                    // You can specify options for more control: e.g., { year: 'numeric', month: 'long', day: 'numeric' }
                    formattedHireDate = dateToFormat.toLocaleDateString(undefined, {
                        year: 'numeric',
                        month: 'long', // e.g., "October"
                        day: 'numeric'
                    });
                } else {
                    console.warn("hireDate resulted in an invalid Date object:", userData.hireDate);
                    formattedHireDate = String(userData.hireDate); // Show raw if it's not a valid date
                }

                employeeHireDateEl.textContent = formattedHireDate;

            } catch (e) {
                console.error("Error formatting hire date:", userData.hireDate, e);
                employeeHireDateEl.textContent = String(userData.hireDate); // Show raw data if any error occurs
            }
        } else {
            employeeHireDateEl.textContent = '-'; // If userData.hireDate is null, undefined, or empty string
        }
    }

    if (employeeDateOfBirth) {
        if (userData.dateOfBirth && userData.dateOfBirth !== null) { // Explicitly check for null
            try {
                let formattedDOB = '-'; // Default to '-' if formatting fails
                let dobToFormat;

                if (Array.isArray(userData.dateOfBirth) && userData.dateOfBirth.length >= 3) {
                    // Assuming [year, month, dayOfMonth, ...]
                    dobToFormat = new Date(userData.dateOfBirth[0], userData.dateOfBirth[1] - 1, userData.dateOfBirth[2]);
                } else if (typeof userData.dateOfBirth === 'string') {
                    const dateStr = userData.dateOfBirth.split('T')[0]; // Get "YYYY-MM-DD" part
                    const parts = dateStr.split('-');
                    if (parts.length === 3) {
                        dobToFormat = new Date(Number(parts[0]), Number(parts[1]) - 1, Number(parts[2]));
                    } else {
                        dobToFormat = new Date(userData.dateOfBirth);
                    }
                }

                if (dobToFormat && !isNaN(dobToFormat.getTime())) {
                    formattedDOB = dobToFormat.toLocaleDateString(undefined, {
                        year: 'numeric',
                        month: 'long',
                        day: 'numeric'
                    });
                } else {
                    console.warn("dateOfBirth resulted in an invalid Date object:", userData.dateOfBirth);
                    formattedDOB = String(userData.dateOfBirth);
                }

                employeeDateOfBirth.textContent = formattedDOB;

            } catch (e) {
                console.error("Error formatting date of birth:", userData.dateOfBirth, e);
                employeeDateOfBirth.textContent = String(userData.dateOfBirth);
            }
        } else {
            employeeDateOfBirth.textContent = '-'; // If userData.dateOfBirth is null, undefined, or empty string
        }
    }

    if(employeeSalary) {
        employeeSalary.textContent = userData.salary || '-';
    }
}

function setupEventListeners(userData) {
    const editProfileLink = document.getElementById('editProfileLink');
    if (editProfileLink) {
        editProfileLink.addEventListener('click', function(event) {
            event.preventDefault();
            // For now, this is a placeholder.
            // In a real app, this might redirect to an edit profile page
            // or open a modal.
            alert('Edit profile functionality is not yet implemented.\nYour ID is: ' + userData.id);
            // Example redirect: window.location.href = `edit_profile.html?id=${userData.id}`;
        });
    }

    // Add event listeners for other quick links if they need JS interaction
    // For example, if "Request Leave" opens a modal:
    // const requestLeaveBtn = document.querySelector('a[href="#request-leave"]');
    // if (requestLeaveBtn) {
    //     requestLeaveBtn.addEventListener('click', (e) => {
    //         e.preventDefault();
    //         openLeaveRequestModal(); // You'd need to define this function and modal HTML
    //     });
    // }
}

// Optional: Function to load announcements if you implement that feature
/*
async function loadAnnouncements() {
    const announcementsListEl = document.getElementById('announcementsList');
    const announcementsSectionEl = document.querySelector('.announcements-section');

    if (!announcementsListEl || !announcementsSectionEl) return;

    try {
        // const announcements = await makeApiCall('/api/announcements', 'GET'); // Example endpoint
        // For testing, using mock data:
        const announcements = [
            { id: 1, title: "System Maintenance Scheduled", date: "2023-11-15", content: "Please note that there will be a system maintenance on Nov 20th from 2 AM to 4 AM." },
            { id: 2, title: "Annual Company Picnic", date: "2023-11-10", content: "Join us for the annual company picnic on Dec 5th at Central Park!" }
        ];


        if (announcements && announcements.length > 0) {
            announcementsListEl.innerHTML = ''; // Clear "No announcements"
            announcements.forEach(ann => {
                const itemDiv = document.createElement('div');
                itemDiv.className = 'announcement-item'; // Style this class in style.css
                itemDiv.innerHTML = `
                    <h4>${ann.title} <small>(${new Date(ann.date).toLocaleDateString()})</small></h4>
                    <p>${ann.content}</p>
                `;
                announcementsListEl.appendChild(itemDiv);
            });
            announcementsSectionEl.style.display = 'block'; // Show the section
        } else {
            announcementsListEl.innerHTML = '<p>No current announcements.</p>';
            announcementsSectionEl.style.display = 'block'; // Still show section with "no announcements" msg
        }
    } catch (error) {
        console.error('Error loading announcements:', error);
        announcementsListEl.innerHTML = '<p>Could not load announcements at this time.</p>';
        announcementsSectionEl.style.display = 'block';
    }
}
*/