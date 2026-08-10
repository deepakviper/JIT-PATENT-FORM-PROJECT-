import React from 'react';
import { Sparkles } from 'lucide-react';

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
                <Sparkles className="patent-fillers-icon" />
                <span className="patent-fillers-text">JIT PATENTEZY</span>
            </div>
        </header>
    );
}

export default Header;