/**
 * BSIC Notification System
 * Système de notifications professionnelles pour les actions de mise à jour, validation et rejet
 */

class NotificationSystem {
    constructor() {
        this.container = null;
        this.notifications = [];
        this.init();
    }

    init() {
        // Créer le conteneur de notifications
        this.container = document.createElement('div');
        this.container.className = 'notification-container';
        document.body.appendChild(this.container);
    }

    /**
     * Afficher une notification
     * @param {string} type - success, error, warning, info
     * @param {string} title - Titre de la notification
     * @param {string} message - Message détaillé
     * @param {number} duration - Durée en millisecondes (0 pour ne pas fermer automatiquement)
     */
    show(type, title, message, duration = 5000) {
        const notification = this.createNotification(type, title, message);
        this.container.appendChild(notification);
        this.notifications.push(notification);

        // Animation d'entrée
        setTimeout(() => {
            notification.style.animation = 'slideIn 0.3s ease-out';
        }, 10);

        // Fermeture automatique
        if (duration > 0) {
            setTimeout(() => {
                this.hide(notification);
            }, duration);
        }

        return notification;
    }

    createNotification(type, title, message) {
        const notification = document.createElement('div');
        notification.className = `notification ${type}`;

        const icons = {
            success: '✓',
            error: '✕',
            warning: '⚠',
            info: 'ℹ'
        };

        notification.innerHTML = `
            <div class="notification-icon">${icons[type]}</div>
            <div class="notification-content">
                <div class="notification-title">${title}</div>
                <div class="notification-message">${message}</div>
            </div>
            <button class="notification-close" onclick="notificationSystem.hide(this.parentElement)">×</button>
        `;

        return notification;
    }

    hide(notification) {
        if (!notification || !notification.parentElement) return;

        notification.classList.add('hiding');
        setTimeout(() => {
            if (notification.parentElement) {
                notification.parentElement.removeChild(notification);
            }
            const index = this.notifications.indexOf(notification);
            if (index > -1) {
                this.notifications.splice(index, 1);
            }
        }, 300);
    }

    hideAll() {
        this.notifications.forEach(notification => {
            this.hide(notification);
        });
    }

    // Messages prédéfinis pour les actions courantes
    success(message) {
        return this.show('success', 'Opération réussie', message);
    }

    error(message) {
        return this.show('error', 'Erreur', message);
    }

    warning(message) {
        return this.show('warning', 'Attention', message);
    }

    info(message) {
        return this.show('info', 'Information', message);
    }

    // Messages spécifiques pour les actions BSIC
    clientCreated(clientName) {
        return this.success(`Le client ${clientName} a été créé avec succès`);
    }

    clientUpdated(clientName) {
        return this.success(`Les informations du client ${clientName} ont été mises à jour`);
    }

    clientDeleted(clientName) {
        return this.warning(`Le client ${clientName} a été supprimé`);
    }

    loanApproved(loanId, clientName) {
        return this.success(`Le prêt ${loanId} pour ${clientName} a été approuvé`);
    }

    loanRejected(loanId, clientName) {
        return this.warning(`Le prêt ${loanId} pour ${clientName} a été rejeté`);
    }

    transactionProcessed(transactionId, amount) {
        return this.success(`La transaction ${transactionId} de ${amount} FCFA a été traitée`);
    }

    reportGenerated(reportType) {
        return this.success(`Le rapport ${reportType} a été généré avec succès`);
    }

    dataSaved(entity) {
        return this.success(`Les données de ${entity} ont été enregistrées`);
    }

    validationSuccess(entity) {
        return this.success(`La validation de ${entity} a été effectuée avec succès`);
    }

    rejectionConfirmed(entity) {
        return this.warning(`Le rejet de ${entity} a été confirmé`);
    }
}

/**
 * Système de confirmation pour les actions critiques
 */
class ConfirmationSystem {
    constructor() {
        this.overlay = null;
    }

    /**
     * Afficher une boîte de dialogue de confirmation
     * @param {string} type - warning, danger, info
     * @param {string} title - Titre de la confirmation
     * @param {string} message - Message de confirmation
     * @param {string} details - Détails supplémentaires
     * @param {Function} onConfirm - Fonction à exécuter lors de la confirmation
     * @param {Function} onCancel - Fonction à exécuter lors de l'annulation
     */
    show(type, title, message, details = '', onConfirm = null, onCancel = null) {
        const overlay = this.createConfirmation(type, title, message, details, onConfirm, onCancel);
        document.body.appendChild(overlay);

        // Animation d'entrée
        setTimeout(() => {
            overlay.style.display = 'flex';
        }, 10);

        return overlay;
    }

    createConfirmation(type, title, message, details, onConfirm, onCancel) {
        const overlay = document.createElement('div');
        overlay.className = 'confirmation-overlay';
        overlay.style.display = 'none';

        const icons = {
            warning: '⚠',
            danger: '🗑',
            info: 'ℹ'
        };

        overlay.innerHTML = `
            <div class="confirmation-dialog">
                <div class="confirmation-header">
                    <div class="confirmation-icon ${type}">${icons[type]}</div>
                    <div class="confirmation-title">${title}</div>
                </div>
                <div class="confirmation-body">
                    <div class="confirmation-message">${message}</div>
                    ${details ? `<div class="confirmation-details">${details}</div>` : ''}
                </div>
                <div class="confirmation-footer">
                    <button class="btn-confirm btn-confirm-secondary" onclick="this.closest('.confirmation-overlay').remove(); ${onCancel ? `(${onCancel})()` : ''}">
                        Annuler
                    </button>
                    <button class="btn-confirm ${type === 'danger' ? 'btn-confirm-danger' : 'btn-confirm-primary'}" onclick="this.closest('.confirmation-overlay').remove(); ${onConfirm ? `(${onConfirm})()` : ''}">
                        Confirmer
                    </button>
                </div>
            </div>
        `;

        // Fermer en cliquant sur l'overlay
        overlay.addEventListener('click', (e) => {
            if (e.target === overlay) {
                overlay.remove();
                if (onCancel) onCancel();
            }
        });

        return overlay;
    }

    // Confirmations prédéfinies
    confirmDelete(entityName, onConfirm) {
        return this.show(
            'danger',
            'Confirmation de suppression',
            `Êtes-vous sûr de vouloir supprimer ${entityName} ?`,
            'Cette action est irréversible et affectera les données associées.',
            onConfirm
        );
    }

    confirmReject(entityName, onConfirm) {
        return this.show(
            'warning',
            'Confirmation de rejet',
            `Êtes-vous sûr de vouloir rejeter ${entityName} ?`,
            'Un email de notification sera envoyé aux parties concernées.',
            onConfirm
        );
    }

    confirmApproval(entityName, onConfirm) {
        return this.show(
            'info',
            'Confirmation d\'approbation',
            `Voulez-vous approuver ${entityName} ?`,
            'Cette action finalisera le processus de validation.',
            onConfirm
        );
    }
}

/**
 * Gestionnaire des actions avec progression
 */
class ActionManager {
    constructor() {
        this.notificationSystem = new NotificationSystem();
        this.confirmationSystem = new ConfirmationSystem();
    }

    /**
     * Exécuter une action avec notification de progression
     * @param {Function} action - L'action à exécuter
     * @param {string} loadingMessage - Message pendant le chargement
     * @param {string} successMessage - Message de succès
     * @param {string} errorMessage - Message d'erreur
     */
    async executeAction(action, loadingMessage, successMessage, errorMessage) {
        // Afficher la notification de chargement
        const loadingNotification = this.notificationSystem.show('info', 'Traitement en cours', loadingMessage, 0);

        try {
            // Ajouter l'indicateur de progression
            const progressIndicator = document.createElement('span');
            progressIndicator.className = 'action-progress';
            loadingNotification.querySelector('.notification-content').appendChild(progressIndicator);

            // Exécuter l'action
            const result = await action();

            // Retirer l'indicateur de progression
            progressIndicator.remove();

            // Afficher le succès
            this.notificationSystem.hide(loadingNotification);
            this.notificationSystem.success(successMessage);

            return result;
        } catch (error) {
            // Retirer l'indicateur de progression
            const progressElement = loadingNotification.querySelector('.action-progress');
            if (progressElement) progressElement.remove();

            // Afficher l'erreur
            this.notificationSystem.hide(loadingNotification);
            this.notificationSystem.error(errorMessage + ': ' + error.message);

            throw error;
        }
    }

    // Actions prédéfinies
    async updateClient(clientId, clientData) {
        return this.executeAction(
            async () => {
                // Simuler une API call
                await new Promise(resolve => setTimeout(resolve, 1500));
                return { id: clientId, ...clientData };
            },
            'Mise à jour des informations du client...',
            `Le client ${clientData.name} a été mis à jour avec succès`,
            'Erreur lors de la mise à jour du client'
        );
    }

    async approveLoan(loanId) {
        return this.executeAction(
            async () => {
                await new Promise(resolve => setTimeout(resolve, 2000));
                return { id: loanId, status: 'approved' };
            },
            'Approbation du prêt en cours...',
            `Le prêt ${loanId} a été approuvé avec succès`,
            'Erreur lors de l\'approbation du prêt'
        );
    }

    async rejectLoan(loanId, reason) {
        return this.executeAction(
            async () => {
                await new Promise(resolve => setTimeout(resolve, 1500));
                return { id: loanId, status: 'rejected', reason };
            },
            'Rejet du prêt en cours...',
            `Le prêt ${loanId} a été rejeté`,
            'Erreur lors du rejet du prêt'
        );
    }
}

// Initialisation globale
const notificationSystem = new NotificationSystem();
const confirmationSystem = new ConfirmationSystem();
const actionManager = new ActionManager();

// Export pour utilisation dans d'autres modules
if (typeof module !== 'undefined' && module.exports) {
    module.exports = {
        NotificationSystem,
        ConfirmationSystem,
        ActionManager,
        notificationSystem,
        confirmationSystem,
        actionManager
    };
}
