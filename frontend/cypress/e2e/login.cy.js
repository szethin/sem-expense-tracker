describe('User Login Module', () => {
  beforeEach(() => {
    cy.visit('http://localhost:3000/auth/login');
  });

  it('Test Case 1: Successful login with valid credentials', () => {
    // Intercept the login API call
    cy.intercept('POST', '**/mywallet/auth/signin', {
      statusCode: 200,
      body: {
        id: 1,
        email: 'testuser@example.com',
        roles: ['ROLE_USER'],
        accessToken: 'mock-token-123'
      }
    }).as('loginRequest');

    // Type valid credentials
    cy.get('input[name="email"]').type('testuser@example.com');
    cy.get('input[name="password"]').type('password123');

    // Click login button
    cy.get('input[type="submit"]').click();

    // Assert that the login API was called
    cy.wait('@loginRequest');

    // Assert successful navigation to dashboard
    cy.url().should('include', '/user/dashboard', { timeout: 10000 });
  });

  it('Test Case 2: Client-side validation with empty fields', () => {
    // Click login button without filling any fields
    cy.get('input[type="submit"]').click();

    // Assert that email required error is visible
    cy.contains('small', 'Email is required!').should('be.visible');

    // Assert that password required error is visible
    cy.contains('small', 'Password is required!').should('be.visible');

    // Verify that no API call was made (user should not navigate)
    cy.url().should('include', '/auth/login');
  });

  it('Test Case 3: Invalid credentials error banner', () => {
    // Intercept the login API call with bad credentials response
    cy.intercept('POST', '**/mywallet/auth/signin', {
      statusCode: 401,
      body: {
        message: 'Bad credentials'
      }
    }).as('failedLoginRequest');

    // Type an unregistered email and wrong password
    cy.get('input[name="email"]').type('nonexistent@example.com');
    cy.get('input[name="password"]').type('wrongpassword');

    // Click login button
    cy.get('input[type="submit"]').click();

    // Assert that the login API was called
    cy.wait('@failedLoginRequest');

    // Assert that the error banner with invalid credentials message appears
    cy.get('.auth-form p').should('be.visible').and('contain', 'Invalid email or password!');

    // Verify that user is still on the login page
    cy.url().should('include', '/auth/login');
  });
});
