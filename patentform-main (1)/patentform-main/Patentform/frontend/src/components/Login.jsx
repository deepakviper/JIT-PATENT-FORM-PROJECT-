import React, { useState } from 'react';
import { User, Mail, Users, ArrowRight } from 'lucide-react';

function Login({ onLogin }) {
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!name.trim()) {
      alert('Please enter your full name.');
      return;
    }
    if (!email.trim() || !email.includes('@')) {
      alert('Please enter a valid email address.');
      return;
    }

    onLogin({
      name: name.trim(),
      email: email.trim(),
    });
  };

  return (
    <div className="login-overlay">
      <div className="login-card">
        <div className="login-header">
          <div className="login-logo-container">
            <img
              src="logojit.jpeg"
              alt="JIT Logo"
              className="login-institution-logo"
            />
            <span className="patent-fillers-text" style={{ color: '#ffffff' }}>JIT PATENTEZ</span>
          </div>
          <h2 className="login-title">Welcome to Patent Filing Portal</h2>
          <p className="login-subtitle">
            Jeppiaar Institute of Technology. Please enter your credentials and applicant details to proceed to the workspace.
          </p>
        </div>

        <form onSubmit={handleSubmit} className="login-form">
          <div className="form-group">
            <label className="form-label" htmlFor="applicant-name">
              Full Name *
            </label>
            <div className="input-container">
              <User className="input-icon" />
              <input
                id="applicant-name"
                type="text"
                className="login-input"
                placeholder="Enter applicant's full name"
                value={name}
                onChange={(e) => setName(e.target.value)}
                required
              />
            </div>
          </div>

          <div className="form-group">
            <label className="form-label" htmlFor="applicant-email">
              Email Address *
            </label>
            <div className="input-container">
              <Mail className="input-icon" />
              <input
                id="applicant-email"
                type="email"
                className="login-input"
                placeholder="Enter applicant's email address"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
              />
            </div>
          </div>

          <button type="submit" className="login-btn">
            Enter Workspace
            <ArrowRight size={18} />
          </button>
        </form>
      </div>
    </div>
  );
}

export default Login;
