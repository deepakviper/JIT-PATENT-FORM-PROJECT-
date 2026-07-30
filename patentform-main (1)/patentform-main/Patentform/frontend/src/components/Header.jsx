    import React from 'react';
    import { Sparkles, LogOut } from 'lucide-react';

    function Header({ user, onLogout }) {
    return (
        <header className="header">
        <div className="header-left">
            <img
            src="logojit.jpeg"
            alt="Jeppiaar Institute of Technology Logo"
            className="institution-logo"
            />
            
            <div className="institution-info">
            <div className="institution-name">
                JEPPIAAR INSTITUTE OF TECHNOLOGY (AUTONOMOUS)
            </div>
            <div className="institution-address">
                Kunnam, TK, Sunguvarchatram, Sriperumbudur - Chennai - 631 604
            </div>
            <div className="institution-accreditation">
                Affiliated to Anna University, Chennai, approved by AICTE and certified with ISO 9001:2015.
            </div>
            <div className="institution-accreditation">
                ARIIA 2020-Secured all India rank 6th-25th (Band A)
            </div>
            </div>
        </div>
        <div className="header-right">
            {user && (
              <div className="user-badge-container">
                <div className="user-avatar">
                  {user.name.charAt(0).toUpperCase()}
                </div>
                <div className="user-details">
                  <span className="user-badge-name">{user.name}</span>
                  <span className="user-badge-email">
                    {user.email} {user.extraPersonsCount > 0 ? `(${user.extraPersonsCount} inventor${user.extraPersonsCount > 1 ? 's' : ''})` : ''}
                  </span>
                </div>
                <button className="user-logout-btn" onClick={onLogout} title="Switch User / Sign Out">
                  <LogOut size={16} />
                </button>
              </div>
            )}
            <Sparkles className="patent-fillers-icon" />
            <span className="patent-fillers-text">JIT PATENTEZ</span>
        </div>
        </header>
    );
    }

    export default Header;